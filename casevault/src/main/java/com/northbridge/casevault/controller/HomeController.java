package com.northbridge.casevault.controller;

import com.northbridge.casevault.service.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    private final AuditService auditService;

    public HomeController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        auditService.record(authentication.getName(), "VIEW_HOME", "/", "SUCCESS");
        return "Logged in as " + authentication.getName()
                + " with role " + authentication.getAuthorities()
                + " | audit events recorded: " + auditService.countEvents();
    }
}