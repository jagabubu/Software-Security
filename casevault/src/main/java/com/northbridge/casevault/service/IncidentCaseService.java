package com.northbridge.casevault.service;

import com.northbridge.casevault.exception.NotFoundException;
import com.northbridge.casevault.model.Category;
import com.northbridge.casevault.model.IncidentCase;
import com.northbridge.casevault.model.Role;
import com.northbridge.casevault.model.Severity;
import com.northbridge.casevault.model.User;
import com.northbridge.casevault.repository.IncidentCaseRepository;
import com.northbridge.casevault.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IncidentCaseService {

    private final IncidentCaseRepository caseRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public IncidentCaseService(IncidentCaseRepository caseRepository,
                               UserRepository userRepository,
                               AuditService auditService) {
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public IncidentCase createCase(String username, String title, String description,
                                   Category category, Severity severity) {
        User user = loadUser(username);

        if (user.getRole() != Role.CLIENT) {
            auditService.record(username, "CREATE_CASE", "cases", "DENIED");
            throw new AccessDeniedException("Not allowed");
        }

        // The organisation comes from the logged-in user, never from the form
        IncidentCase saved = caseRepository.save(new IncidentCase(
                title.trim(), description.trim(), category, severity,
                user.getOrganisation(), user));

        auditService.record(username, "CREATE_CASE", "case:" + saved.getId(), "SUCCESS");
        return saved;
    }

    public IncidentCase getCase(Long caseId, String username) {
        User user = loadUser(username);
        String target = "case:" + caseId;

        if (user.getRole() != Role.CLIENT) {
            auditService.record(username, "VIEW_CASE", target, "DENIED");
            throw new NotFoundException("Case not found");
        }

        Optional<IncidentCase> result =
                caseRepository.findByIdAndOrganisationId(caseId, user.getOrganisation().getId());

        if (result.isEmpty()) {
            auditService.record(username, "VIEW_CASE", target, "DENIED");
            throw new NotFoundException("Case not found");
        }

        auditService.record(username, "VIEW_CASE", target, "SUCCESS");
        return result.get();
    }

    public List<IncidentCase> listCases(String username) {
        User user = loadUser(username);

        if (user.getRole() != Role.CLIENT) {
            auditService.record(username, "LIST_CASES", "cases", "DENIED");
            return List.of();
        }

        auditService.record(username, "LIST_CASES", "cases", "SUCCESS");
        return caseRepository.findAllByOrganisationIdOrderByCreatedAtDesc(
                user.getOrganisation().getId());
    }

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Not found"));
    }
}