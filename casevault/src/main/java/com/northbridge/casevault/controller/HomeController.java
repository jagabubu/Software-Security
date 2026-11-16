package com.northbridge.casevault.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        boolean isAdmin = hasRole(authentication, "ROLE_AUDITOR_ADMIN");
        boolean isClient = hasRole(authentication, "ROLE_CLIENT");

        model.addAttribute("username", authentication.getName());
        model.addAttribute("role", authentication.getAuthorities().toString());
        model.addAttribute("canSeeCases", !isAdmin);     // auditors get no case content
        model.addAttribute("canCreateCase", isClient);
        model.addAttribute("isAdmin", isAdmin);
        return "home";
    }

    // These flags only decide which links to SHOW. The security rules are enforced elsewhere.
    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}