package com.northbridge.casevault.service;

import com.northbridge.casevault.exception.NotFoundException;
import com.northbridge.casevault.model.IncidentCase;
import com.northbridge.casevault.model.Role;
import com.northbridge.casevault.model.User;
import com.northbridge.casevault.repository.IncidentCaseRepository;
import com.northbridge.casevault.repository.UserRepository;
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

    public IncidentCase getCase(Long caseId, String username) {
        User user = loadUser(username);
        String target = "case:" + caseId;

        // Staff roles get access in a later step; for now only clients can view cases
        if (user.getRole() != Role.CLIENT) {
            auditService.record(username, "VIEW_CASE", target, "DENIED");
            throw new NotFoundException("Case not found");
        }

        // THE ORGANISATION CHECK: id AND the logged-in user's organisation
        Optional<IncidentCase> result =
                caseRepository.findByIdAndOrganisationId(caseId, user.getOrganisation().getId());   // BLANK

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