package com.landgreet.admin;

import com.landgreet.user.AppUserDetails;
import com.landgreet.user.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAllForAdmin());
        return "admin/users";
    }

    @PostMapping("/{id}/active")
    public String setActive(
            @AuthenticationPrincipal AppUserDetails principal,
            @PathVariable long id,
            @RequestParam boolean active,
            RedirectAttributes redirect) {
        try {
            userService.setActive(principal.getId(), id, active);
            redirect.addFlashAttribute("ok", active ? "Account reactivated." : "Account deactivated.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/admin")
    public String setAdmin(
            @AuthenticationPrincipal AppUserDetails principal,
            @PathVariable long id,
            @RequestParam boolean admin,
            RedirectAttributes redirect) {
        try {
            userService.setAdmin(principal.getId(), id, admin);
            redirect.addFlashAttribute("ok", admin ? "Promoted to admin." : "Admin role removed.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
