package com.pickupdrop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class SupportDto {

    public record PostRequest(
            @NotBlank @Size(max = 1000) String body
    ) {
    }

    /** One chat line, as either side sees it. */
    @Getter
    @NoArgsConstructor
    public static class MessageResponse {
        private String id;
        private String authorName;
        /** True for the team's messages, whoever is reading. */
        private boolean staff;
        /** True when the reader wrote it. */
        private boolean mine;
        private String body;
        private LocalDateTime createdAt;

        public MessageResponse(String id, String authorName, boolean staff, boolean mine,
                               String body, LocalDateTime createdAt) {
            this.id = id;
            this.authorName = authorName;
            this.staff = staff;
            this.mine = mine;
            this.body = body;
            this.createdAt = createdAt;
        }
    }

    /** A traveller's own thread. */
    @Getter
    @NoArgsConstructor
    public static class ThreadResponse {
        private List<MessageResponse> messages;
        private int unread;

        public ThreadResponse(List<MessageResponse> messages, int unread) {
            this.messages = messages;
            this.unread = unread;
        }
    }

    /** Operator inbox row — who is waiting, and for how long. */
    @Getter
    @NoArgsConstructor
    public static class InboxRowResponse {
        private String userId;
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private int messageCount;
        private int unread;
        private LocalDateTime lastMessageAt;

        public InboxRowResponse(String userId, String customerName, String customerEmail,
                                String customerPhone, int messageCount, int unread,
                                LocalDateTime lastMessageAt) {
            this.userId = userId;
            this.customerName = customerName;
            this.customerEmail = customerEmail;
            this.customerPhone = customerPhone;
            this.messageCount = messageCount;
            this.unread = unread;
            this.lastMessageAt = lastMessageAt;
        }
    }
}
