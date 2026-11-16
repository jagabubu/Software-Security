package com.northbridge.casevault.controller;

import com.northbridge.casevault.exception.InvalidTransitionException;
import com.northbridge.casevault.model.IncidentCase;
import com.northbridge.casevault.model.Severity;
import com.northbridge.casevault.model.Status;
import com.northbridge.casevault.service.CaseWorkService;
import com.northbridge.casevault.service.IncidentCaseService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class IncidentCaseController {

    private final IncidentCaseService caseService;
    private final CaseWorkService workService;

    public IncidentCaseController(IncidentCaseService caseService, CaseWorkService workService) {
        this.caseService = caseService;
        this.workService = workService;
    }

    @GetMapping("/cases")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Status status,
                       @RequestParam(required = false) Severity severity,
                       Authentication authentication,
                       Model model) {
        model.addAttribute("cases",
                caseService.searchCases(authentication.getName(), q, status, severity));
        model.addAttribute("q", q);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSeverity", severity);
        model.addAttribute("statuses", Status.values());
        model.addAttribute("severities", Severity.values());
        return "cases";
    }

    @GetMapping("/cases/{id}")
    public String view(@PathVariable Long id, Authentication authentication, Model model) {
        String username = authentication.getName();
        IncidentCase incident = caseService.getCase(id, username);
        model.addAttribute("incident", incident);

        // These flags only decide what to SHOW. The services enforce the rules regardless.
        boolean isManager = hasRole(authentication, "ROLE_CASE_MANAGER");
        boolean isAnalyst = hasRole(authentication, "ROLE_ANALYST");
        model.addAttribute("canAssign", isManager);
        model.addAttribute("canChangeStatus", isManager);
        model.addAttribute("canWork", isAnalyst);
        model.addAttribute("nextStatuses", isManager ? incident.allowedNextStatuses() : List.of());

        if (isManager) {
            model.addAttribute("analysts", caseService.listAssignableAnalysts(username));
        }
        model.addAttribute("notes", workService.listNotes(username, id));
        model.addAttribute("actions", workService.listActions(username, id));
        return "case-detail";
    }

    @PostMapping("/cases/{id}/assign")
    public String assign(@PathVariable Long id,
                         @RequestParam String analyst,
                         Authentication authentication) {
        caseService.assignAnalyst(authentication.getName(), id, analyst);
        return "redirect:/cases/" + id;
    }

    @PostMapping("/cases/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam Status status,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            caseService.changeStatus(authentication.getName(), id, status);
        } catch (InvalidTransitionException e) {
            redirectAttributes.addFlashAttribute("error", "That status change is not allowed.");
        }
        return "redirect:/cases/" + id;
    }

    @PostMapping("/cases/{id}/notes")
    public String addNote(@PathVariable Long id,
                          @Valid NoteForm form,
                          BindingResult bindingResult,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "A note must be 1 to 2000 characters.");
            return "redirect:/cases/" + id;
        }
        workService.addNote(authentication.getName(), id, form.getContent());
        return "redirect:/cases/" + id;
    }

    @PostMapping("/cases/{id}/actions")
    public String addAction(@PathVariable Long id,
                            @Valid ActionForm form,
                            BindingResult bindingResult,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "An action must be 1 to 500 characters.");
            return "redirect:/cases/" + id;
        }
        workService.addAction(authentication.getName(), id, form.getDescription());
        return "redirect:/cases/" + id;
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}