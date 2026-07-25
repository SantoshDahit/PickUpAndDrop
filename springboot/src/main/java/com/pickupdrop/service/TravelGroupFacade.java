package com.pickupdrop.service;

import com.pickupdrop.dto.GroupMessageDto;
import com.pickupdrop.dto.TravelGroupDto;
import com.pickupdrop.entity.Booking;
import com.pickupdrop.entity.GroupMessage;
import com.pickupdrop.entity.TravelGroup;
import com.pickupdrop.exception.ApiException;
import com.pickupdrop.exception.ErrorCode;
import com.pickupdrop.mapper.DriverMapper;
import com.pickupdrop.mapper.RouteMapper;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TravelGroupFacade {

    private final TravelGroupService travelGroupService;
    private final BookingService bookingService;
    private final GroupMessageService groupMessageService;
    private final UserService userService;
    private final BookingFacade bookingFacade;
    private final RouteMapper routeMapper;
    private final DriverMapper driverMapper;

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

        return new TravelGroupDto.Response(group.getId(), routeMapper.toResponse(group.getRoute()),
                group.getStatus(), memberResponses, agreedDate,
                driverMapper.toPublicResponse(group.getDriver()));
    }

    @Transactional(readOnly = true)
    public List<GroupMessageDto.Response> getMessages(String userId, boolean isAdmin, String groupId) {
        travelGroupService.getById(groupId);
        requireMembershipOrAdmin(bookingService.getActiveByGroupId(groupId), userId, isAdmin);
        return groupMessageService.getAllByGroupId(groupId).stream()
                .map(message -> new GroupMessageDto.Response(
                        message.getId(),
                        firstName(message.getUser().getName()),
                        message.getBody(),
                        message.getUser().getId().equals(userId),
                        message.getCreatedAt()))
                .toList();
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
        return new GroupMessageDto.Response(message.getId(), firstName(message.getUser().getName()),
                message.getBody(), true, message.getCreatedAt());
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
