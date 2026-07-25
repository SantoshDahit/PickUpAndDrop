package com.landgreet.config;

import com.landgreet.user.AppUserDetails;
import com.landgreet.user.User;
import com.landgreet.user.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Exposes the fresh DB-backed user to every template as ${currentUser}. */
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserService userService;

    public GlobalModelAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("currentUser")
    public User currentUser(@AuthenticationPrincipal AppUserDetails principal) {
        if (principal == null) {
            return null;
        }
        return userService.findActive(principal.getId()).orElse(null);
    }
}
