package com.northbridge.casevault.controller;

import com.northbridge.casevault.model.IncidentCase;
import com.northbridge.casevault.service.IncidentCaseService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class IncidentCaseController {

    private final IncidentCaseService caseService;

    public IncidentCaseController(IncidentCaseService caseService) {
        this.caseService = caseService;
    }

    @GetMapping(value = "/cases", produces = "text/plain")
    public String list(Authentication authentication) {
        List<IncidentCase> cases = caseService.listCases(authentication.getName());
        StringBuilder text = new StringBuilder("Your cases:\n");
        for (IncidentCase c : cases) {
            text.append(c.getId()).append(" | ")
                    .append(c.getTitle()).append(" | ")
                    .append(c.getSeverity()).append(" | ")
                    .append(c.getStatus()).append("\n");
        }
        return text.toString();
    }

    @GetMapping(value = "/cases/{id}", produces = "text/plain")
    public String view(@PathVariable Long id, Authentication authentication) {
        IncidentCase c = caseService.getCase(id, authentication.getName());
        return "Case " + c.getId() + "\n"
                + "Title: " + c.getTitle() + "\n"
                + "Category: " + c.getCategory() + "\n"
                + "Severity: " + c.getSeverity() + "\n"
                + "Status: " + c.getStatus() + "\n"
                + "Description: " + c.getDescription() + "\n";
    }
}