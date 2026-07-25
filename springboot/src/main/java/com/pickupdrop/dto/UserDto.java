package com.pickupdrop.dto;

import com.pickupdrop.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserDto {

    public record PatchRequest(
            @Size(max = 100) String name,
            @Size(max = 30) String phone
    ) {
    }

    public record PasswordPatchRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 6, max = 200) String newPassword
    ) {
    }

    public record DeleteRequest(
            @NotBlank String password
    ) {
    }

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String id;
        private String email;
        private String name;
        private String phone;
        private Role role;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
