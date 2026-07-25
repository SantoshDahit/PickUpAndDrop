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
        private LocalDateTime createdAt;

        public Response(String id, String authorFirstName, String body, boolean mine,
                        LocalDateTime createdAt) {
            this.id = id;
            this.authorFirstName = authorFirstName;
            this.body = body;
            this.mine = mine;
            this.createdAt = createdAt;
        }
    }
}
