package com.landgreet.user.web;

import com.landgreet.user.AppUserDetails;
import com.landgreet.user.DuplicateEmailException;
import com.landgreet.user.InvalidImageException;
import com.landgreet.user.User;
import com.landgreet.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String account(@AuthenticationPrincipal AppUserDetails principal, Model model) {
        User user = currentUser(principal);
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", prefilled(user));
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordForm());
        }
        model.addAttribute("user", user);
        return "account/index";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @AuthenticationPrincipal AppUserDetails principal,
            @Valid @ModelAttribute("profileForm") ProfileForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            try {
                userService.updateProfile(principal.getId(), form.getName(), form.getEmail(), form.getPhone());
                redirect.addFlashAttribute("ok", "Profile saved.");
                return "redirect:/account";
            } catch (DuplicateEmailException e) {
                binding.rejectValue("email", "duplicate", "That email is already registered.");
            }
        }
        model.addAttribute("user", currentUser(principal));
        model.addAttribute("passwordForm", new PasswordForm());
        return "account/index";
    }

    @PostMapping("/avatar")
    public String updateAvatar(
            @AuthenticationPrincipal AppUserDetails principal,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirect) {
        if (file == null || file.isEmpty()) {
            redirect.addFlashAttribute("error", "Please choose an image first.");
            return "redirect:/account";
        }
        try {
            userService.updateAvatar(principal.getId(), file.getInputStream());
            redirect.addFlashAttribute("ok", "Photo updated.");
        } catch (InvalidImageException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return "redirect:/account";
    }

    @PostMapping("/avatar/delete")
    public String removeAvatar(@AuthenticationPrincipal AppUserDetails principal, RedirectAttributes redirect) {
        userService.removeAvatar(principal.getId());
        redirect.addFlashAttribute("ok", "Photo removed.");
        return "redirect:/account";
    }

    @PostMapping("/password")
    public String changePassword(
            @AuthenticationPrincipal AppUserDetails principal,
            @Valid @ModelAttribute("passwordForm") PasswordForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect) {
        if (!binding.hasErrors() && !form.getNewPassword().equals(form.getConfirmPassword())) {
            binding.rejectValue("confirmPassword", "mismatch", "New passwords don't match.");
        }
        if (!binding.hasErrors()
                && !userService.changePassword(principal.getId(), form.getCurrentPassword(), form.getNewPassword())) {
            binding.rejectValue("currentPassword", "wrong", "Current password is wrong.");
        }
        if (binding.hasErrors()) {
            User user = currentUser(principal);
            model.addAttribute("user", user);
            model.addAttribute("profileForm", prefilled(user));
            return "account/index";
        }
        redirect.addFlashAttribute("ok", "Password changed.");
        return "redirect:/account";
    }

    @PostMapping("/delete")
    public String deleteAccount(
            @AuthenticationPrincipal AppUserDetails principal,
            @RequestParam("password") String password,
            HttpServletRequest request,
            RedirectAttributes redirect) throws Exception {
        if (!userService.softDeleteSelf(principal.getId(), password)) {
            redirect.addFlashAttribute("error", "That password didn't match — account not deleted.");
            return "redirect:/account";
        }
        request.logout();
        return "redirect:/";
    }

    private static ProfileForm prefilled(User user) {
        var form = new ProfileForm();
        form.setName(user.getName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        return form;
    }

    private User currentUser(AppUserDetails principal) {
        return userService.findActive(principal.getId())
                .orElseThrow(() -> new IllegalStateException("Session for missing user " + principal.getId()));
    }
}
