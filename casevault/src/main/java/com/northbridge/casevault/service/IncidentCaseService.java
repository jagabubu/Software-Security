package com.northbridge.casevault.service;

import com.northbridge.casevault.exception.InvalidTransitionException;
import com.northbridge.casevault.exception.NotFoundException;
import com.northbridge.casevault.model.Category;
import com.northbridge.casevault.model.IncidentCase;
import com.northbridge.casevault.model.Role;
import com.northbridge.casevault.model.Severity;
import com.northbridge.casevault.model.Status;
import com.northbridge.casevault.model.User;
import com.northbridge.casevault.repository.IncidentCaseRepository;
import com.northbridge.casevault.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class IncidentCaseService {

    private static final int MAX_SEARCH_LENGTH = 100;

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

        IncidentCase saved = caseRepository.save(new IncidentCase(
                title.trim(), description.trim(), category, severity,
                user.getOrganisation(), user));

        auditService.record(username, "CREATE_CASE", "case:" + saved.getId(), "SUCCESS");
        return saved;
    }

    public IncidentCase getCase(Long caseId, String username) {
        User user = loadUser(username);
        String target = "case:" + caseId;

        Optional<IncidentCase> result = findVisible(user, caseId);

        if (result.isEmpty()) {
            auditService.record(username, "VIEW_CASE", target, "DENIED");
            throw new NotFoundException("Case not found");
        }

        auditService.record(username, "VIEW_CASE", target, "SUCCESS");
        return result.get();
    }

    public List<IncidentCase> listCases(String username) {
        User user = loadUser(username);
        boolean allowed = mayBrowseCases(user);
        auditService.record(username, "LIST_CASES", "cases", allowed ? "SUCCESS" : "DENIED");
        return visibleCases(user);
    }

    public List<IncidentCase> searchCases(String username, String text,
                                          Status status, Severity severity) {
        User user = loadUser(username);

        if (!mayBrowseCases(user)) {
            auditService.record(username, "SEARCH_CASES", "cases", "DENIED");
            return List.of();
        }

        // STEP 1: only the cases this user is allowed to see.
        List<IncidentCase> visible = visibleCases(user);

        // STEP 2: apply the user's filters to THAT set only.
        String needle = normaliseSearch(text);
        List<IncidentCase> results = new ArrayList<>();
        for (IncidentCase c : visible) {
            if (!needle.isEmpty() && !c.getTitle().toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }
            if (status != null && c.getStatus() != status) {
                continue;
            }
            if (severity != null && c.getSeverity() != severity) {
                continue;
            }
            results.add(c);
        }

        // The search text itself is deliberately NOT written to the audit log
        auditService.record(username, "SEARCH_CASES", "cases", "SUCCESS");
        return results;
    }

    public void assignAnalyst(String managerUsername, Long caseId, String analystUsername) {
        User manager = loadUser(managerUsername);

        if (manager.getRole() != Role.CASE_MANAGER || !manager.isEnabled()) {
            auditService.record(managerUsername, "ASSIGN_CASE", "case:" + caseId, "DENIED");
            throw new AccessDeniedException("Not allowed");
        }

        IncidentCase incident = caseRepository.findById(caseId)
                .orElseThrow(() -> new NotFoundException("Case not found"));

        User analyst = userRepository.findByUsername(analystUsername)
                .filter(u -> u.getRole() == Role.ANALYST && u.isEnabled())
                .orElseThrow(() -> new NotFoundException("Analyst not found"));

        incident.assignTo(analyst);
        caseRepository.save(incident);

        auditService.record(managerUsername, "ASSIGN_CASE",
                "case:" + caseId + " to:" + analystUsername, "SUCCESS");
    }

    public void changeStatus(String managerUsername, Long caseId, Status newStatus) {
        User manager = loadUser(managerUsername);
        String target = "case:" + caseId;

        if (manager.getRole() != Role.CASE_MANAGER || !manager.isEnabled()) {
            auditService.record(managerUsername, "CHANGE_STATUS", target, "DENIED");
            throw new AccessDeniedException("Not allowed");
        }

        IncidentCase incident = caseRepository.findById(caseId)
                .orElseThrow(() -> new NotFoundException("Case not found"));

        Status current = incident.getStatus();
        if (newStatus == null || !incident.allowedNextStatuses().contains(newStatus)) {
            auditService.record(managerUsername, "CHANGE_STATUS", target, "DENIED");
            throw new InvalidTransitionException("Status change not allowed");
        }

        incident.changeStatus(newStatus);
        caseRepository.save(incident);

        auditService.record(managerUsername, "CHANGE_STATUS",
                target + " " + current + "->" + newStatus, "SUCCESS");
    }

    public List<User> listAssignableAnalysts(String username) {
        User user = loadUser(username);
        if (user.getRole() != Role.CASE_MANAGER || !user.isEnabled()) {
            throw new AccessDeniedException("Not allowed");
        }
        return userRepository.findAllByRoleAndEnabledTrue(Role.ANALYST);
    }

    // Lets other services reuse the SAME visibility rule, without duplicating it
    public Optional<IncidentCase> findVisibleCase(String username, Long caseId) {
        return findVisible(loadUser(username), caseId);
    }

    // THE ACCESS RULE for a single case: deny by default
    private Optional<IncidentCase> findVisible(User user, Long caseId) {
        if (!user.isEnabled()) {
            return Optional.empty();
        }
        Role role = user.getRole();
        if (role == Role.CLIENT) {
            return caseRepository.findByIdAndOrganisationId(caseId, user.getOrganisation().getId());
        }
        if (role == Role.ANALYST) {
            return caseRepository.findByIdAndOwnerId(caseId, user.getId());
        }
        if (role == Role.CASE_MANAGER) {
            return caseRepository.findById(caseId);
        }
        return Optional.empty();    // auditors never get case content
    }

    // THE ACCESS RULE for lists: the same logic, for many cases
    private List<IncidentCase> visibleCases(User user) {
        if (!mayBrowseCases(user)) {
            return List.of();
        }
        Role role = user.getRole();
        if (role == Role.CLIENT) {
            return caseRepository.findAllByOrganisationIdOrderByCreatedAtDesc(
                    user.getOrganisation().getId());
        }
        if (role == Role.ANALYST) {
            return caseRepository.findAllByOwnerIdOrderByCreatedAtDesc(user.getId());
        }
        return caseRepository.findAllByOrderByCreatedAtDesc();    // case managers
    }

    private boolean mayBrowseCases(User user) {
        Role role = user.getRole();
        return user.isEnabled()
                && (role == Role.CLIENT || role == Role.ANALYST || role == Role.CASE_MANAGER);
    }

    private String normaliseSearch(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.trim().toLowerCase(Locale.ROOT);
        return cleaned.length() > MAX_SEARCH_LENGTH
                ? cleaned.substring(0, MAX_SEARCH_LENGTH)
                : cleaned;
    }

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Not found"));
    }
}