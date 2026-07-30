package com.pickupdrop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDto {

    public record SignupRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @Size(max = 30) String phone,
            @NotBlank @Size(min = 6, max = 200) String password
    ) {
    }

    public record LoginRequest(
            @NotBlank String email,
            @NotBlank String password
    ) {
    }

    public record ForgotPasswordRequest(
            @NotBlank @Email @Size(max = 254) String email
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 6, max = 200) String password
    ) {
    }

    @Getter
    @NoArgsConstructor
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private UserDto.Response user;

        public TokenResponse(String accessToken, String refreshToken, UserDto.Response user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
        }
    }
}
