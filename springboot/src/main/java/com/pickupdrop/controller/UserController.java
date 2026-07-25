package com.pickupdrop.controller;

import com.pickupdrop.dto.UserDto;
import com.pickupdrop.service.UserFacade;
import com.pickupdrop.util.AuthorizationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserFacade userFacade;

    /** My profile. */
    @GetMapping("/me")
    public UserDto.Response getMe() {
        return userFacade.getMe(AuthorizationUtil.getCurrentUser().getUserId());
    }

    /** Update my name/phone. */
    @PatchMapping("/me")
    public UserDto.Response updateMe(@RequestBody @Valid UserDto.PatchRequest request) {
        return userFacade.updateMe(AuthorizationUtil.getCurrentUser().getUserId(), request);
    }

    /** Change my password (verifies the current one). */
    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(@RequestBody @Valid UserDto.PasswordPatchRequest request) {
        userFacade.updatePassword(AuthorizationUtil.getCurrentUser().getUserId(), request);
    }

    /** Soft-delete my account (password re-auth; cancels active bookings). */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(@RequestBody @Valid UserDto.DeleteRequest request) {
        userFacade.deleteMe(AuthorizationUtil.getCurrentUser().getUserId(), request);
    }
}
