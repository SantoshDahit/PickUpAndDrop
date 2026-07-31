package com.pickupdrop.service;

import com.pickupdrop.dto.SupportDto;
import com.pickupdrop.entity.SupportMessage;
import com.pickupdrop.entity.User;
import com.pickupdrop.exception.ApiException;
import com.pickupdrop.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Traveller ↔ team conversation (plan 014). Group chat needs a group, so a
 * traveller riding individually previously had no way to reach anyone.
 */
@Component
@RequiredArgsConstructor
public class SupportFacade {

    /** How the team appears to a traveller, matching group chat (012 §4.2). */
    public static final String STAFF_AUTHOR_NAME = "Pickup & Drop team";

    private final SupportMessageService supportMessageService;
    private final UserService userService;

    /** My thread. Reading it clears the team's messages as read. */
    @Transactional
    public SupportDto.ThreadResponse getMyThread(String userId) {
        List<SupportMessage> thread = supportMessageService.getThread(userId);
        supportMessageService.markRead(userId, true);
        return new SupportDto.ThreadResponse(
                thread.stream().map(m -> toResponse(m, userId)).toList(), 0);
    }

    @Transactional
    public SupportDto.MessageResponse postFromTraveller(String userId, SupportDto.PostRequest request) {
        User user = userService.getById(userId);
        SupportMessage message = supportMessageService.save(
                new SupportMessage(user, user, false, validBody(request.body())));
        return toResponse(message, userId);
    }

    // ===== Admin =====

    @Transactional(readOnly = true)
    public List<SupportDto.InboxRowResponse> getInbox() {
        return supportMessageService.getInbox().stream()
                .map(row -> new SupportDto.InboxRowResponse(
                        row.userId(), row.name(), row.email(), row.phone(),
                        (int) row.messageCount(), (int) row.unreadFromTraveller(),
                        row.lastMessageAt()))
                .toList();
    }

    /** A traveller's thread as the operator sees it; clears their messages as read. */
    @Transactional
    public SupportDto.ThreadResponse getThreadForAdmin(String adminUserId, String userId) {
        userService.getById(userId);   // 404 if the account is gone
        List<SupportMessage> thread = supportMessageService.getThread(userId);
        supportMessageService.markRead(userId, false);
        return new SupportDto.ThreadResponse(
                thread.stream().map(m -> toResponse(m, adminUserId)).toList(), 0);
    }

    /**
     * Team reply. It lands on the traveller's thread but records which operator
     * wrote it, and is flagged staff at write time.
     */
    @Transactional
    public SupportDto.MessageResponse postFromStaff(String adminUserId, String userId,
                                                    SupportDto.PostRequest request) {
        User traveller = userService.getById(userId);
        User admin = userService.getById(adminUserId);
        SupportMessage message = supportMessageService.save(
                new SupportMessage(traveller, admin, true, validBody(request.body())));
        return toResponse(message, adminUserId);
    }

    private static String validBody(String raw) {
        String body = raw == null ? "" : raw.trim();
        if (body.isEmpty() || body.length() > 1000) {
            throw new ApiException(ErrorCode.MESSAGE_BODY_IS_INVALID);
        }
        return body;
    }

    /**
     * A staff message always shows the team name, never the operator's, so
     * travellers see one consistent counterparty.
     */
    private static SupportDto.MessageResponse toResponse(SupportMessage message, String readerId) {
        return new SupportDto.MessageResponse(
                message.getId(),
                message.isStaff() ? STAFF_AUTHOR_NAME : message.getAuthor().getName(),
                message.isStaff(),
                message.getAuthor().getId().equals(readerId),
                message.getBody(),
                message.getCreatedAt());
    }
}
