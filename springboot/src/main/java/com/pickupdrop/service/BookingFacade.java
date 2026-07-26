package com.pickupdrop.service;

import com.pickupdrop.domain.GroupMatcher;
import com.pickupdrop.dto.BookingDto;
import com.pickupdrop.entity.Booking;
import com.pickupdrop.entity.Route;
import com.pickupdrop.entity.TravelGroup;
import com.pickupdrop.entity.User;
import com.pickupdrop.enums.BookingStatus;
import com.pickupdrop.enums.GroupStatus;
import com.pickupdrop.enums.MatchPref;
import com.pickupdrop.exception.ApiException;
import com.pickupdrop.exception.ErrorCode;
import com.pickupdrop.mapper.BookingMapper;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BookingFacade {

    private final BookingService bookingService;
    private final RouteService routeService;
    private final TravelGroupService travelGroupService;
    private final UserService userService;
    private final GroupMatcher groupMatcher;
    private final BookingMapper bookingMapper;

    @Transactional
    public BookingDto.Response create(String userId, BookingDto.PostRequest request) {
        validateTravelDate(request.travelDate());
        User user = userService.getById(userId);
        Route route = routeService.getActiveById(request.routeId());

        Booking booking = new Booking(user, route, request.travelDate(),
                blankToNull(request.flightNo()), request.partySize(), request.matchPref(),
                blankToNull(request.intro()), blankToNull(request.contact()), blankToNull(request.notes()));

        boolean joinedExisting = false;
        if (request.groupId() != null && !request.groupId().isBlank()) {
            // Explicit join of an admin-published ride (public groups only).
            TravelGroup ride = joinablePublicRide(request.groupId(), request.travelDate(), request.partySize());
            booking.forceGroupPref();
            booking.joinGroup(ride);
            bookingService.save(booking);
            refreshGroupStatus(ride);
            joinedExisting = true;
        } else if (request.matchPref() == MatchPref.GROUP) {
            TravelGroup group = findQualifyingGroup(route.getId(), request.travelDate(), request.partySize());
            if (group == null) {
                group = travelGroupService.save(new TravelGroup(route));
            } else {
                joinedExisting = true;
            }
            booking.joinGroup(group);
            bookingService.save(booking);
            refreshGroupStatus(group);
        } else {
            bookingService.save(booking);
        }

        BookingDto.Response response = bookingMapper.toResponse(booking);
        response.setJoinedExistingGroup(joinedExisting);
        return response;
    }

    /** Oldest open group on the route that keeps the date span and seats within policy. */
    private TravelGroup findQualifyingGroup(String routeId, LocalDate travelDate, int partySize) {
        for (TravelGroup group : travelGroupService.getOpenByRouteId(routeId)) {
            List<Booking> members = bookingService.getActiveByGroupId(group.getId());
            if (groupMatcher.qualifies(members, group.getTargetDate(), travelDate, partySize)) {
                return group;
            }
        }
        return null;
    }

    /** Only admin-published OPEN rides are joinable by id — organic groups stay private. */
    private TravelGroup joinablePublicRide(String groupId, LocalDate travelDate, int partySize) {
        TravelGroup ride;
        try {
            ride = travelGroupService.getById(groupId);
        } catch (ApiException e) {
            throw new ApiException(ErrorCode.GROUP_NOT_JOINABLE);
        }
        if (!ride.isPublicRide() || ride.getStatus() != GroupStatus.OPEN) {
            throw new ApiException(ErrorCode.GROUP_NOT_JOINABLE);
        }
        List<Booking> members = bookingService.getActiveByGroupId(ride.getId());
        if (groupMatcher.seatsOf(members) + partySize > TravelGroup.MAX_SEATS) {
            throw new ApiException(ErrorCode.GROUP_SEATS_FULL);
        }
        if (!groupMatcher.qualifies(members, ride.getTargetDate(), travelDate, partySize)) {
            throw new ApiException(ErrorCode.GROUP_DATE_OUT_OF_WINDOW);
        }
        return ride;
    }

    @Transactional(readOnly = true)
    public List<BookingDto.SummaryResponse> getMyBookings(String userId) {
        return bookingService.getAllByUserId(userId).stream()
                .map(bookingMapper::toSummaryResponse)
                .toList();
    }

    @Transactional
    public BookingDto.Response updateTravelDate(String userId, String bookingId, BookingDto.PatchRequest request) {
        validateTravelDate(request.travelDate());
        Booking booking = ownedActiveBooking(userId, bookingId);
        booking.updateTravelDate(request.travelDate());
        return bookingMapper.toResponse(booking);
    }

    @Transactional
    public void cancel(String userId, String bookingId) {
        Booking booking = ownedActiveBooking(userId, bookingId);
        cancelInternal(booking);
    }

    /** Shared with UserFacade (account deletion cancels active bookings). */
    @Transactional
    public void cancelInternal(Booking booking) {
        TravelGroup group = booking.getTravelGroup();
        booking.cancel();
        bookingService.save(booking);
        if (group != null) {
            refreshGroupStatus(group);
        }
    }

    @Transactional
    public void refreshGroupStatus(TravelGroup group) {
        List<Booking> members = bookingService.getActiveByGroupId(group.getId());
        GroupStatus next;
        if (members.isEmpty()) {
            // Organic groups die when empty; published rides were born empty
            // and stay browsable until the admin closes them.
            next = group.isPublicRide() ? GroupStatus.OPEN : GroupStatus.CLOSED;
        } else if (groupMatcher.seatsOf(members) >= TravelGroup.MAX_SEATS) {
            next = GroupStatus.FULL;
        } else {
            next = GroupStatus.OPEN;
        }
        if (next != group.getStatus()) {
            group.updateStatus(next);
            travelGroupService.save(group);
        }
    }

    @Transactional(readOnly = true)
    public Page<BookingDto.SummaryResponse> search(BookingDto.SearchRequest searchRequest, Pageable pageable) {
        return bookingService.search(searchRequest, pageable).map(bookingMapper::toSummaryResponse);
    }

    private Booking ownedActiveBooking(String userId, String bookingId) {
        Booking booking = bookingService.getById(bookingId);
        if (!booking.getUser().getId().equals(userId)) {
            // 404, not 403 — do not confirm the booking exists to non-owners.
            throw new ApiException(ErrorCode.BOOKING_IS_NOT_FOUND);
        }
        if (!booking.isActive()) {
            throw new ApiException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }
        return booking;
    }

    private static void validateTravelDate(LocalDate travelDate) {
        LocalDate today = LocalDate.now();
        if (travelDate.isBefore(today) || travelDate.isAfter(today.plusDays(365))) {
            throw new ApiException(ErrorCode.BOOKING_DATE_IS_INVALID);
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
