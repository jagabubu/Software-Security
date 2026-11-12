package com.northbridge.casevault.service;

import com.northbridge.casevault.exception.NotFoundException;
import com.northbridge.casevault.model.CaseNote;
import com.northbridge.casevault.model.IncidentCase;
import com.northbridge.casevault.model.RemediationAction;
import com.northbridge.casevault.model.Role;
import com.northbridge.casevault.model.User;
import com.northbridge.casevault.repository.CaseNoteRepository;
import com.northbridge.casevault.repository.RemediationActionRepository;
import com.northbridge.casevault.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CaseWorkService {

    private final IncidentCaseService caseService;
    private final UserRepository userRepository;
    private final CaseNoteRepository noteRepository;
    private final RemediationActionRepository actionRepository;
    private final AuditService auditService;

    public CaseWorkService(IncidentCaseService caseService,
                           UserRepository userRepository,
                           CaseNoteRepository noteRepository,
                           RemediationActionRepository actionRepository,
                           AuditService auditService) {
        this.caseService = caseService;
        this.userRepository = userRepository;
        this.noteRepository = noteRepository;
        this.actionRepository = actionRepository;
        this.auditService = auditService;
    }

    public void addNote(String username, Long caseId, String content) {
        IncidentCase incident = requireAnalystOnVisibleCase(username, caseId, "ADD_NOTE");
        User author = loadUser(username);
        noteRepository.save(new CaseNote(incident, author, content.trim()));
        auditService.record(username, "ADD_NOTE", "case:" + caseId, "SUCCESS");
    }

    public void addAction(String username, Long caseId, String description) {
        IncidentCase incident = requireAnalystOnVisibleCase(username, caseId, "ADD_ACTION");
        User author = loadUser(username);
        actionRepository.save(new RemediationAction(incident, author, description.trim()));
        auditService.record(username, "ADD_ACTION", "case:" + caseId, "SUCCESS");
    }

    // Notes are INTERNAL: only analysts and managers who can see the case may read them
    public List<CaseNote> listNotes(String username, Long caseId) {
        Role role = loadUser(username).getRole();
        if (role != Role.ANALYST && role != Role.CASE_MANAGER) {
            return List.of();
        }
        if (caseService.findVisibleCase(username, caseId).isEmpty()) {
            return List.of();
        }
        return noteRepository.findAllByIncidentCaseIdOrderByCreatedAtAsc(caseId);
    }

    // Remediation actions are SHARED with anyone who can see the case
    public List<RemediationAction> listActions(String username, Long caseId) {
        if (caseService.findVisibleCase(username, caseId).isEmpty()) {
            return List.of();
        }
        return actionRepository.findAllByIncidentCaseIdOrderByCreatedAtAsc(caseId);
    }

    private IncidentCase requireAnalystOnVisibleCase(String username, Long caseId, String action) {
        User user = loadUser(username);
        String target = "case:" + caseId;

        Optional<IncidentCase> visible = caseService.findVisibleCase(username, caseId);
        if (visible.isEmpty()) {
            auditService.record(username, action, target, "DENIED");
            throw new NotFoundException("Case not found");
        }
        if (user.getRole() != Role.ANALYST) {
            auditService.record(username, action, target, "DENIED");
            throw new AccessDeniedException("Not allowed");
        }
        return visible.get();
    }

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Not found"));
    }
}