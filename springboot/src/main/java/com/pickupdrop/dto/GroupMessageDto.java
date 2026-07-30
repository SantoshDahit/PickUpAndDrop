package com.pickupdrop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class GroupMessageDto {

    public record PostRequest(
            @NotBlank @Size(max = 1000) String body
    ) {
    }

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String id;
        private String authorFirstName;
        private String body;
        private boolean mine;
        /** Posted by the operator — the client shows the team name, not a person. */
        private boolean staff;
        private LocalDateTime createdAt;

        public Response(String id, String authorFirstName, String body, boolean mine,
                        boolean staff, LocalDateTime createdAt) {
            this.id = id;
            this.authorFirstName = authorFirstName;
            this.body = body;
            this.mine = mine;
            this.staff = staff;
            this.createdAt = createdAt;
        }
    }

    /**
     * Transcript line for the admin console: the real author, not a first name.
     * Never returned by a customer-facing route (plan 012 §4.3).
     */
    @Getter
    @NoArgsConstructor
    public static class AdminResponse {
        private String id;
        private String authorName;
        private String authorEmail;
        private String body;
        private boolean staff;
        private LocalDateTime createdAt;

        public AdminResponse(String id, String authorName, String authorEmail, String body,
                             boolean staff, LocalDateTime createdAt) {
            this.id = id;
            this.authorName = authorName;
            this.authorEmail = authorEmail;
            this.body = body;
            this.staff = staff;
            this.createdAt = createdAt;
        }
    }
}
