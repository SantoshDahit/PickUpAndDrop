package com.pickupdrop.security.service;

import com.pickupdrop.dto.AuthDto;
import com.pickupdrop.entity.User;
import com.pickupdrop.enums.EmailPurpose;
import com.pickupdrop.enums.Role;
import com.pickupdrop.exception.ApiException;
import com.pickupdrop.exception.ErrorCode;
import com.pickupdrop.mapper.UserMapper;
import com.pickupdrop.security.jwt.JwtTokenProvider;
import com.pickupdrop.service.UserService;
import com.pickupdrop.service.mail.AfterCommitExecutor;
import com.pickupdrop.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AuthFacade {

    private final UserService userService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MailService mailService;
    private final AfterCommitExecutor afterCommitExecutor;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public AuthDto.TokenResponse signup(AuthDto.SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (userService.existsActiveByEmail(email)) {
            throw new ApiException(ErrorCode.USER_DUPLICATE_EMAIL);
        }
        User user = userService.save(new User(
                email,
                passwordEncoder.encode(request.password()),
                request.name().trim(),
                blankToNull(request.phone()),
                Role.USER));
        afterCommitExecutor.execute(() -> mailService.sendWelcome(user.getEmail(), user.getName()));
        return tokenResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        User user;
        try {
            user = userService.getActiveByEmail(normalizeEmail(request.email()));
        } catch (ApiException e) {
            // Same error as a wrong password — no account-state oracle.
            throw new ApiException(ErrorCode.LOGIN_FAILED);
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(ErrorCode.LOGIN_FAILED);
        }
        return tokenResponse(user);
    }

    /**
     * Emails a reset link when the address has an account.
     *
     * <p>Returns normally either way: telling the caller whether the address is
     * registered would turn this into an account-enumeration endpoint.
     */
    @Transactional
    public void forgotPassword(AuthDto.ForgotPasswordRequest request) {
        // Optional, not catch: a thrown ApiException would mark this
        // transaction rollback-only and turn the no-op case into a 500.
        // Nullable lookup, not catch: a thrown ApiException would mark this
        // transaction rollback-only and turn the no-op case into a 500.
        User user = userService.getNullableActiveByEmail(normalizeEmail(request.email()));
        if (user == null) {
            return;
        }
        // Code row must be committed before the link reaches the inbox.
        String rawCode = emailVerificationService.issue(user, EmailPurpose.PASSWORD_RESET);
        afterCommitExecutor.execute(() -> mailService.sendPasswordReset(
                user.getEmail(), user.getName(), rawCode));
    }

    /** Redeems a reset code and sets the new password. The code is single-use. */
    @Transactional
    public void resetPassword(AuthDto.ResetPasswordRequest request) {
        User user = emailVerificationService.redeem(request.token(), EmailPurpose.PASSWORD_RESET);
        user.updatePassword(passwordEncoder.encode(request.password()));
        userService.save(user);
    }

    private AuthDto.TokenResponse tokenResponse(User user) {
        return new AuthDto.TokenResponse(
                jwtTokenProvider.createAccessToken(user.getId(), user.getEmail()),
                jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail()),
                userMapper.toResponse(user));
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
