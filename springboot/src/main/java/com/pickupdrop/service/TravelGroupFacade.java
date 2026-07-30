package com.pickupdrop.service;

import com.pickupdrop.dto.GroupMessageDto;
import com.pickupdrop.dto.TravelGroupDto;
import com.pickupdrop.entity.Booking;
import com.pickupdrop.entity.GroupMessage;
import com.pickupdrop.domain.WeekBucket;
import com.pickupdrop.entity.Route;
import com.pickupdrop.entity.TravelGroup;
import com.pickupdrop.enums.GroupStatus;
import com.pickupdrop.exception.ApiException;
import com.pickupdrop.exception.ErrorCode;
import com.pickupdrop.mapper.DriverMapper;
import com.pickupdrop.mapper.RouteMapper;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TravelGroupFacade {

    /** How the operator appears in a traveller's chat (plan 012 §4.2). */
    public static final String STAFF_AUTHOR_NAME = "Pickup & Drop team";

    private static final int PREVIEW_LENGTH = 120;

    private final TravelGroupService travelGroupService;
    private final BookingService bookingService;
    private final GroupMessageService groupMessageService;
    private final UserService userService;
    private final BookingFacade bookingFacade;
    private final RouteService routeService;
    private final RouteMapper routeMapper;
    private final DriverMapper driverMapper;

    // ===== Published rides (plan 006) =====

    @Transactional
    public TravelGroupDto.OpenRideResponse publishRide(TravelGroupDto.AdminPostRequest request) {
        LocalDate today = LocalDate.now();
        if (request.targetDate().isBefore(today) || request.targetDate().isAfter(today.plusDays(365))) {
            throw new ApiException(ErrorCode.BOOKING_DATE_IS_INVALID);
        }
        Route route = routeService.getActiveById(request.routeId());
        TravelGroup ride = travelGroupService.save(TravelGroup.published(
                route, request.targetDate(), WeekBucket.of(request.targetDate())));
        return toOpenRide(ride, List.of());
    }

    @Transactional
    public void closeRide(String groupId) {
        TravelGroup ride = travelGroupService.getById(groupId);
        if (!bookingService.getActiveByGroupId(groupId).isEmpty()) {
            throw new ApiException(ErrorCode.GROUP_HAS_MEMBERS);
        }
        ride.updateStatus(GroupStatus.CLOSED);
        travelGroupService.save(ride);
    }

    /** Browse card list — no personal data, ever. */
    @Transactional(readOnly = true)
    public List<TravelGroupDto.OpenRideResponse> listOpenRides() {
        LocalDate today = LocalDate.now();
        return travelGroupService.getOpenPublicRides().stream()
                .filter(ride -> !ride.getTargetDate().isBefore(today))
                .map(ride -> toOpenRide(ride, bookingService.getActiveByGroupId(ride.getId())))
                .filter(card -> card.getSeatsLeft() > 0)
                .toList();
    }

    private TravelGroupDto.OpenRideResponse toOpenRide(TravelGroup ride, List<Booking> members) {
        int seats = members.stream().mapToInt(Booking::getPartySize).sum();
        return new TravelGroupDto.OpenRideResponse(
                ride.getId(), routeMapper.toResponse(ride.getRoute()), ride.getTargetDate(),
                members.size(), TravelGroup.MAX_SEATS - seats,
                members.stream().map(Booking::getTravelDate).min(java.util.Comparator.naturalOrder()).orElse(null),
                members.stream().map(Booking::getTravelDate).max(java.util.Comparator.naturalOrder()).orElse(null));
    }

    @Transactional(readOnly = true)
    public TravelGroupDto.Response getById(String userId, boolean isAdmin, String groupId) {
        TravelGroup group = travelGroupService.getById(groupId);
        List<Booking> members = bookingService.getActiveByGroupId(groupId);
        requireMembershipOrAdmin(members, userId, isAdmin);

        List<TravelGroupDto.MemberResponse> memberResponses = members.stream()
                .map(member -> new TravelGroupDto.MemberResponse(
                        firstNameOf(member),
                        member.getPartySize(),
                        member.getTravelDate(),
                        member.getIntro(),
                        member.getUser().getId().equals(userId)))
                .toList();

        LocalDate agreedDate = members.size() > 1
                && members.stream().map(Booking::getTravelDate).distinct().count() == 1
                ? members.get(0).getTravelDate() : null;

        String bucket = group.getWeekBucket();
        return new TravelGroupDto.Response(group.getId(), routeMapper.toResponse(group.getRoute()),
                group.getStatus(), memberResponses, agreedDate,
                driverMapper.toPublicResponse(group.getDriver()),
                bucket == null ? null : WeekBucket.startOf(bucket),
                bucket == null ? null : WeekBucket.endOf(bucket));
    }

    @Transactional(readOnly = true)
    public List<GroupMessageDto.Response> getMessages(String userId, boolean isAdmin, String groupId) {
        travelGroupService.getById(groupId);
        requireMembershipOrAdmin(bookingService.getActiveByGroupId(groupId), userId, isAdmin);
        return groupMessageService.getAllByGroupId(groupId).stream()
                .map(message -> toMemberView(message, userId))
                .toList();
    }

    /**
     * Chat line as a traveller sees it. A staff reply carries the team name
     * instead of the operator's first name, and is never "mine".
     */
    private static GroupMessageDto.Response toMemberView(GroupMessage message, String userId) {
        boolean staff = message.isStaff();
        return new GroupMessageDto.Response(
                message.getId(),
                staff ? STAFF_AUTHOR_NAME : firstName(message.getUser().getName()),
                message.getBody(),
                !staff && message.getUser().getId().equals(userId),
                staff,
                message.getCreatedAt());
    }

    @Transactional
    public GroupMessageDto.Response postMessage(String userId, String groupId, GroupMessageDto.PostRequest request) {
        TravelGroup group = travelGroupService.getById(groupId);
        requireMembership(bookingService.getActiveByGroupId(groupId), userId);
        String body = request.body() == null ? "" : request.body().trim();
        if (body.isEmpty() || body.length() > 1000) {
            throw new ApiException(ErrorCode.MESSAGE_BODY_IS_INVALID);
        }
        GroupMessage message = groupMessageService.save(
                new GroupMessage(group, userService.getById(userId), body));
        return toMemberView(message, userId);
    }

    // ===== Admin chat moderation (plan 012) =====

    /** Chat index: every group, most recently active first. */
    @Transactional(readOnly = true)
    public List<TravelGroupDto.ChatSummaryResponse> listChats() {
        Map<String, Long> counts = groupMessageService.countByGroupId();
        Map<String, GroupMessage> latest = groupMessageService.getLatestByGroupId();

        return travelGroupService.getAllForAdminIndex().stream()
                .map(group -> {
                    List<Booking> members = bookingService.getActiveByGroupId(group.getId());
                    GroupMessage last = latest.get(group.getId());
                    String bucket = group.getWeekBucket();
                    return new TravelGroupDto.ChatSummaryResponse(
                            group.getId(),
                            routeMapper.toResponse(group.getRoute()),
                            group.getStatus(),
                            group.isPublicRide(),
                            group.getTargetDate(),
                            bucket == null ? null : WeekBucket.startOf(bucket),
                            bucket == null ? null : WeekBucket.endOf(bucket),
                            members.size(),
                            TravelGroup.MAX_SEATS - seatsOf(members),
                            counts.getOrDefault(group.getId(), 0L).intValue(),
                            last == null ? null : preview(last.getBody()),
                            last == null ? null
                                    : last.isStaff() ? STAFF_AUTHOR_NAME : last.getUser().getName(),
                            last != null && last.isStaff(),
                            last == null ? null : last.getCreatedAt(),
                            group.getDriver() != null);
                })
                // A silent group still needs attention, so groups without messages
                // sort by their own recency (createdAt order) after the chatty ones.
                .sorted(Comparator.comparing(
                                TravelGroupDto.ChatSummaryResponse::getLastMessageAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** Full group detail for the operator — real names and contact details. */
    @Transactional(readOnly = true)
    public TravelGroupDto.AdminDetailResponse getAdminDetail(String groupId) {
        TravelGroup group = travelGroupService.getById(groupId);
        List<Booking> members = bookingService.getActiveByGroupId(groupId);

        List<TravelGroupDto.AdminMemberResponse> memberResponses = members.stream()
                .map(member -> new TravelGroupDto.AdminMemberResponse(
                        member.getId(),
                        member.getUser().getId(),
                        member.getUser().getName(),
                        member.getUser().getEmail(),
                        member.getUser().getPhone(),
                        member.getContact(),
                        member.getPartySize(),
                        member.getTravelDate(),
                        member.getFlightNo(),
                        member.getIntro(),
                        member.getNotes()))
                .toList();

        LocalDate agreedDate = members.size() > 1
                && members.stream().map(Booking::getTravelDate).distinct().count() == 1
                ? members.get(0).getTravelDate() : null;

        String bucket = group.getWeekBucket();
        return new TravelGroupDto.AdminDetailResponse(
                group.getId(),
                routeMapper.toResponse(group.getRoute()),
                group.getStatus(),
                group.isPublicRide(),
                group.getTargetDate(),
                bucket == null ? null : WeekBucket.startOf(bucket),
                bucket == null ? null : WeekBucket.endOf(bucket),
                TravelGroup.MAX_SEATS - seatsOf(members),
                agreedDate,
                driverMapper.toPublicResponse(group.getDriver()),
                memberResponses);
    }

    /** Transcript with real authors — admin console only. */
    @Transactional(readOnly = true)
    public List<GroupMessageDto.AdminResponse> getAdminMessages(String groupId) {
        travelGroupService.getById(groupId);
        return groupMessageService.getAllByGroupId(groupId).stream()
                .map(message -> new GroupMessageDto.AdminResponse(
                        message.getId(),
                        message.getUser().getName(),
                        message.getUser().getEmail(),
                        message.getBody(),
                        message.isStaff(),
                        message.getCreatedAt()))
                .toList();
    }

    /**
     * Operator reply. Flagged {@code staff} at write time so travellers see the
     * team name; the admin does not need to be a member of the group.
     */
    @Transactional
    public GroupMessageDto.AdminResponse postStaffMessage(String adminUserId, String groupId,
                                                          GroupMessageDto.PostRequest request) {
        TravelGroup group = travelGroupService.getById(groupId);
        String body = request.body() == null ? "" : request.body().trim();
        if (body.isEmpty() || body.length() > 1000) {
            throw new ApiException(ErrorCode.MESSAGE_BODY_IS_INVALID);
        }
        GroupMessage message = groupMessageService.save(
                new GroupMessage(group, userService.getById(adminUserId), body, true));
        return new GroupMessageDto.AdminResponse(message.getId(), message.getUser().getName(),
                message.getUser().getEmail(), message.getBody(), true, message.getCreatedAt());
    }

    /**
     * Bookings the operator could add: same route, same landing week, seats fit,
     * not already in this group. Pre-filtered so the console only offers valid
     * choices — {@link #addMember} re-checks anyway (plan 012 §4.1).
     */
    @Transactional(readOnly = true)
    public List<TravelGroupDto.CandidateResponse> listAddCandidates(String groupId) {
        TravelGroup group = travelGroupService.getById(groupId);
        String bucket = group.getWeekBucket();
        if (bucket == null) {
            return List.of();
        }
        List<Booking> members = bookingService.getActiveByGroupId(groupId);
        int seatsLeft = TravelGroup.MAX_SEATS - seatsOf(members);

        return bookingService.getActiveByRouteIdAndWeek(
                        group.getRoute().getId(), WeekBucket.startOf(bucket), WeekBucket.endOf(bucket)).stream()
                .filter(booking -> booking.getTravelGroup() == null
                        || !booking.getTravelGroup().getId().equals(groupId))
                .filter(booking -> booking.getPartySize() <= seatsLeft)
                .map(booking -> new TravelGroupDto.CandidateResponse(
                        booking.getId(),
                        booking.getUser().getName(),
                        booking.getUser().getEmail(),
                        booking.getPartySize(),
                        booking.getTravelDate(),
                        booking.getFlightNo(),
                        booking.getTravelGroup() == null ? null : booking.getTravelGroup().getId()))
                .toList();
    }

    /**
     * Attaches a booking to this group, applying the traveller join rules —
     * same route, same landing week, seats must fit (plan 012 §4.1). A booking
     * already in another group is moved, and that group's status is refreshed.
     */
    @Transactional
    public void addMember(String groupId, TravelGroupDto.AdminAddMemberRequest request) {
        TravelGroup group = travelGroupService.getById(groupId);
        Booking booking = bookingService.getById(request.bookingId());

        if (!booking.isActive()) {
            throw new ApiException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }
        TravelGroup previous = booking.getTravelGroup();
        if (previous != null && previous.getId().equals(groupId)) {
            return; // already a member — adding twice is a no-op, not a duplicate
        }
        if (!booking.getRoute().getId().equals(group.getRoute().getId())) {
            throw new ApiException(ErrorCode.GROUP_NOT_JOINABLE);
        }
        if (!WeekBucket.of(booking.getTravelDate()).equals(group.getWeekBucket())) {
            throw new ApiException(ErrorCode.GROUP_DATE_OUT_OF_WINDOW);
        }
        List<Booking> members = bookingService.getActiveByGroupId(groupId);
        if (seatsOf(members) + booking.getPartySize() > TravelGroup.MAX_SEATS) {
            throw new ApiException(ErrorCode.GROUP_SEATS_FULL);
        }

        booking.forceGroupPref();
        booking.joinGroup(group);
        bookingService.save(booking);
        bookingFacade.refreshGroupStatus(group);
        if (previous != null) {
            bookingFacade.refreshGroupStatus(previous);
        }
    }

    /**
     * Detaches a booking from this group. The booking stays active and travels
     * individually — cancelling is a different action (plan 012 §4.4).
     */
    @Transactional
    public void removeMember(String groupId, String bookingId) {
        TravelGroup group = travelGroupService.getById(groupId);
        Booking membership = bookingService.getActiveByGroupId(groupId).stream()
                .filter(booking -> booking.getId().equals(bookingId))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.BOOKING_IS_NOT_FOUND));
        membership.leaveGroup();
        bookingService.save(membership);
        bookingFacade.refreshGroupStatus(group);
    }

    private static int seatsOf(List<Booking> members) {
        return members.stream().mapToInt(Booking::getPartySize).sum();
    }

    private static String preview(String body) {
        String single = body.replaceAll("\\s+", " ").trim();
        return single.length() <= PREVIEW_LENGTH ? single
                : single.substring(0, PREVIEW_LENGTH - 1) + "…";
    }

    /** Leaving flips the member's booking to individual and frees the seats. */
    @Transactional
    public void leave(String userId, String groupId) {
        TravelGroup group = travelGroupService.getById(groupId);
        Booking membership = bookingService.getActiveByGroupId(groupId).stream()
                .filter(booking -> booking.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.GROUP_MEMBERSHIP_REQUIRED));
        membership.leaveGroup();
        bookingService.save(membership);
        bookingFacade.refreshGroupStatus(group);
    }

    private static void requireMembershipOrAdmin(List<Booking> members, String userId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        requireMembership(members, userId);
    }

    private static void requireMembership(List<Booking> members, String userId) {
        boolean isMember = members.stream().anyMatch(b -> b.getUser().getId().equals(userId));
        if (!isMember) {
            // 404, not 403 — do not confirm the group exists.
            throw new ApiException(ErrorCode.GROUP_MEMBERSHIP_REQUIRED);
        }
    }

    private static String firstNameOf(Booking booking) {
        return firstName(booking.getUser().getName());
    }

    private static String firstName(String name) {
        String trimmed = name == null ? "" : name.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }
}
