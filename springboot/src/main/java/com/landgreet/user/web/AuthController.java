package com.landgreet.user.web;

import com.landgreet.user.AppUserDetails;
import com.landgreet.user.DuplicateEmailException;
import com.landgreet.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signup(@ModelAttribute("signupForm") SignupForm form) {
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(
            @Valid @ModelAttribute("signupForm") SignupForm form,
            BindingResult binding,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (binding.hasErrors()) {
            return "auth/signup";
        }

        com.landgreet.user.User user;
        try {
            user = userService.register(form.getName(), form.getEmail(), form.getPhone(), form.getPassword());
        } catch (DuplicateEmailException e) {
            binding.rejectValue("email", "duplicate", "That email is already registered. Try logging in.");
            return "auth/signup";
        }

        loginAs(user, request, response);
        return "redirect:/trips";
    }

    private void loginAs(com.landgreet.user.User user, HttpServletRequest request, HttpServletResponse response) {
        var principal = new AppUserDetails(user);
        var auth = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
