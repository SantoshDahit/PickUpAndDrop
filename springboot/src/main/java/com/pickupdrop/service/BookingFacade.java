package com.pickupdrop.service;

import com.pickupdrop.domain.WeekBucket;
import com.pickupdrop.dto.BookingDto;
import com.pickupdrop.dto.TravelGroupDto;
import com.pickupdrop.entity.Booking;
import com.pickupdrop.entity.PriceTier;
import com.pickupdrop.entity.Route;
import com.pickupdrop.entity.TravelGroup;
import com.pickupdrop.entity.User;
import com.pickupdrop.enums.BookingStatus;
import com.pickupdrop.enums.GroupStatus;
import com.pickupdrop.enums.MatchPref;
import com.pickupdrop.exception.ApiException;
import com.pickupdrop.exception.ErrorCode;
import com.pickupdrop.mapper.BookingMapper;
import com.pickupdrop.service.mail.AfterCommitExecutor;
import com.pickupdrop.service.mail.MailService;
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
    private final PriceTierService priceTierService;
    private final MailService mailService;
    private final AfterCommitExecutor afterCommitExecutor;
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
            // Explicit join at booking time (published-ride cards on /book).
            TravelGroup group = joinableGroup(request.groupId(), route.getId(),
                    request.travelDate(), request.partySize());
            booking.forceGroupPref();
            booking.joinGroup(group);
            bookingService.save(booking);
            refreshGroupStatus(group);
            joinedExisting = true;
        } else {
            // Book first, group second (plan 008): the user picks from suggestions.
            bookingService.save(booking);
        }

        // Snapshot while the associations are attached; send once committed.
        MailService.BookingMail mail =
                MailService.BookingMail.of(booking, farePerPerson(route.getId(), request.partySize()));
        afterCommitExecutor.execute(() -> mailService.sendBookingConfirmation(mail));

        BookingDto.Response response = bookingMapper.toResponse(booking);
        response.setJoinedExistingGroup(joinedExisting);
        return response;
    }

    /**
     * Fare for this party size: the highest tier at or below it, matching how
     * the public calculator reads the ladder. Null when a route has no tiers.
     */
    private Integer farePerPerson(String routeId, int partySize) {
        Integer price = null;
        for (PriceTier tier : priceTierService.getByRouteIdOrdered(routeId)) {
            if (tier.getGroupSize() <= partySize || price == null) {
                price = tier.getPricePerPerson();
            }
        }
        return price;
    }

    /**
     * A group is joinable for a booking when it is OPEN, on the same route,
     * in the booking's landing-week bucket, and the seats fit (plan 008).
     */
    private TravelGroup joinableGroup(String groupId, String routeId,
                                      LocalDate travelDate, int partySize) {
        TravelGroup group;
        try {
            group = travelGroupService.getById(groupId);
        } catch (ApiException e) {
            throw new ApiException(ErrorCode.GROUP_NOT_JOINABLE);
        }
        // FULL falls through to the seat check for the clearer error message.
        if (group.getStatus() == GroupStatus.CLOSED || !group.getRoute().getId().equals(routeId)) {
            throw new ApiException(ErrorCode.GROUP_NOT_JOINABLE);
        }
        if (!WeekBucket.of(travelDate).equals(group.getWeekBucket())) {
            throw new ApiException(ErrorCode.GROUP_DATE_OUT_OF_WINDOW);
        }
        List<Booking> members = bookingService.getActiveByGroupId(group.getId());
        if (seatsOf(members) + partySize > TravelGroup.MAX_SEATS) {
            throw new ApiException(ErrorCode.GROUP_SEATS_FULL);
        }
        return group;
    }

    /** Joinable groups for this booking's route + landing week. No personal data. */
    @Transactional(readOnly = true)
    public TravelGroupDto.SuggestionsResponse suggestGroups(String userId, String bookingId) {
        Booking booking = ownedActiveBooking(userId, bookingId);
        String bucket = WeekBucket.of(booking.getTravelDate());
        List<TravelGroupDto.SuggestionResponse> groups = travelGroupService
                .getOpenByRouteIdAndWeekBucket(booking.getRoute().getId(), bucket).stream()
                .filter(group -> !group.getId().equals(
                        booking.getTravelGroup() == null ? null : booking.getTravelGroup().getId()))
                .map(group -> {
                    List<Booking> members = bookingService.getActiveByGroupId(group.getId());
                    int seatsLeft = TravelGroup.MAX_SEATS - seatsOf(members);
                    return seatsLeft >= booking.getPartySize()
                            ? new TravelGroupDto.SuggestionResponse(
                                    group.getId(), members.size(), seatsLeft,
                                    members.stream().map(Booking::getTravelDate)
                                            .min(java.util.Comparator.naturalOrder()).orElse(null),
                                    members.stream().map(Booking::getTravelDate)
                                            .max(java.util.Comparator.naturalOrder()).orElse(null),
                                    group.isPublicRide(), group.getTargetDate())
                            : null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        return new TravelGroupDto.SuggestionsResponse(
                WeekBucket.startOf(bucket), WeekBucket.endOf(bucket), groups);
    }

    /**
     * The user picks their group (or starts one with groupId == null).
     * Switching between same-week groups is allowed — it is a selection.
     */
    @Transactional
    public BookingDto.Response selectGroup(String userId, String bookingId, BookingDto.SelectGroupRequest request) {
        Booking booking = ownedActiveBooking(userId, bookingId);
        TravelGroup previous = booking.getTravelGroup();

        TravelGroup target;
        if (request.groupId() == null || request.groupId().isBlank()) {
            target = travelGroupService.save(TravelGroup.forLandingWeek(
                    booking.getRoute(), WeekBucket.of(booking.getTravelDate())));
        } else {
            target = joinableGroup(request.groupId(), booking.getRoute().getId(),
                    booking.getTravelDate(), booking.getPartySize());
        }
        booking.forceGroupPref();
        booking.joinGroup(target);
        bookingService.save(booking);
        refreshGroupStatus(target);
        if (previous != null && !previous.getId().equals(target.getId())) {
            refreshGroupStatus(previous);
        }
        return bookingMapper.toResponse(booking);
    }

    private static int seatsOf(List<Booking> members) {
        return members.stream().mapToInt(Booking::getPartySize).sum();
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

        // Membership must stay inside the landing-week boundary (plan 008):
        // crossing weeks detaches the booking; the UI re-suggests groups.
        TravelGroup group = booking.getTravelGroup();
        if (group != null && !WeekBucket.of(request.travelDate()).equals(group.getWeekBucket())) {
            booking.leaveGroup();
            booking.forceGroupPref(); // intent stays: they still want a group, in the new week
            bookingService.save(booking);
            refreshGroupStatus(group);
        }
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
        } else if (seatsOf(members) >= TravelGroup.MAX_SEATS) {
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

    @Transactional(readOnly = true)
    public BookingDto.AdminDetailResponse getAdminDetail(String bookingId) {
        return bookingMapper.toAdminDetailResponse(bookingService.getById(bookingId));
    }

    /** Admin cancel on the customer's behalf — same group upkeep as a self-cancel. */
    @Transactional
    public void adminCancel(String bookingId) {
        Booking booking = bookingService.getById(bookingId);
        if (!booking.isActive()) {
            throw new ApiException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }
        cancelInternal(booking);
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
