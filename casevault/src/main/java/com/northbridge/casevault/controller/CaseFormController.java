package com.northbridge.casevault.controller;

import com.northbridge.casevault.model.Category;
import com.northbridge.casevault.model.IncidentCase;
import com.northbridge.casevault.model.Severity;
import com.northbridge.casevault.service.IncidentCaseService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CaseFormController {

    private final IncidentCaseService caseService;

    public CaseFormController(IncidentCaseService caseService) {
        this.caseService = caseService;
    }

    @GetMapping("/cases/new")
    public String showForm(Model model) {
        model.addAttribute("form", new NewCaseForm());
        addChoices(model);
        return "new-case";
    }

    @PostMapping("/cases/new")
    public String submit(@Valid @ModelAttribute("form") NewCaseForm form,
                         BindingResult bindingResult,
                         Authentication authentication,
                         Model model) {
        if (bindingResult.hasErrors()) {
            addChoices(model);
            return "new-case";
        }

        IncidentCase created = caseService.createCase(
                authentication.getName(),
                form.getTitle(),
                form.getDescription(),
                form.getCategory(),
                form.getSeverity());

        return "redirect:/cases/" + created.getId();
    }

    private void addChoices(Model model) {
        model.addAttribute("categories", Category.values());
        model.addAttribute("severities", Severity.values());
    }
}