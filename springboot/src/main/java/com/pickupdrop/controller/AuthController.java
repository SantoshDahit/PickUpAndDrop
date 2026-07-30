package com.pickupdrop.controller;

import com.pickupdrop.dto.AuthDto;
import com.pickupdrop.security.service.AuthFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthFacade authFacade;

    /** Sign up and log straight in. */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDto.TokenResponse signup(@RequestBody @Valid AuthDto.SignupRequest request) {
        return authFacade.signup(request);
    }

    /** Email + password login. */
    @PostMapping("/login")
    public AuthDto.TokenResponse login(@RequestBody @Valid AuthDto.LoginRequest request) {
        return authFacade.login(request);
    }

    /**
     * Emails a reset link. Always 204, whether or not the address is
     * registered — the response must not reveal which accounts exist.
     */
    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@RequestBody @Valid AuthDto.ForgotPasswordRequest request) {
        authFacade.forgotPassword(request);
    }

    /** Sets a new password from a reset token. */
    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@RequestBody @Valid AuthDto.ResetPasswordRequest request) {
        authFacade.resetPassword(request);
    }
}
