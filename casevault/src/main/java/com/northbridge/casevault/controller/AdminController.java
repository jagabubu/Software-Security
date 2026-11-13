package com.northbridge.casevault.controller;

import com.northbridge.casevault.service.AdminService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/admin/audit")
    public String audit(Authentication authentication, Model model) {
        model.addAttribute("events", adminService.recentAuditEvents(authentication.getName()));
        return "admin-audit";
    }

    @GetMapping("/admin/users")
    public String users(Authentication authentication, Model model) {
        model.addAttribute("users", adminService.listUsers(authentication.getName()));
        return "admin-users";
    }

    @PostMapping("/admin/users/disable")
    public String disable(@RequestParam String username,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        adminService.disableUser(authentication.getName(), username);
        redirectAttributes.addFlashAttribute("message", "Account disabled: " + username);
        return "redirect:/admin/users";
    }
}