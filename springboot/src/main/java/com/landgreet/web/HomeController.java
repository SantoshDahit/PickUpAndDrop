package com.landgreet.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home/index";
    }

    /** Booking flow arrives with plan 002; stub keeps the post-login landing real. */
    @GetMapping("/trips")
    public String trips() {
        return "trips/index";
    }

    @GetMapping("/admin")
    public String admin() {
        return "redirect:/admin/users";
    }
}
