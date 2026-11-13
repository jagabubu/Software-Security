package com.northbridge.casevault.service;

import com.northbridge.casevault.exception.NotFoundException;
import com.northbridge.casevault.model.AuditEvent;
import com.northbridge.casevault.model.Role;
import com.northbridge.casevault.model.User;
import com.northbridge.casevault.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public AdminService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public List<User> listUsers(String adminUsername) {
        requireAdmin(adminUsername, "VIEW_USERS");
        return userRepository.findAll(Sort.by("username"));
    }

    public List<AuditEvent> recentAuditEvents(String adminUsername) {
        requireAdmin(adminUsername, "VIEW_AUDIT_LOG");
        auditService.record(adminUsername, "VIEW_AUDIT_LOG", "audit", "SUCCESS");
        return auditService.recentEvents();
    }

    public void disableUser(String adminUsername, String targetUsername) {
        User admin = requireAdmin(adminUsername, "DISABLE_ACCOUNT");
        String target = "user:" + targetUsername;

        // An admin cannot disable their own account
        if (admin.getUsername().equals(targetUsername)) {
            auditService.record(adminUsername, "DISABLE_ACCOUNT", target, "DENIED");
            throw new AccessDeniedException("Not allowed");
        }

        User user = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // The last remaining enabled admin cannot be disabled (avoids lock-out)
        if (user.getRole() == Role.AUDITOR_ADMIN
                && userRepository.findAllByRoleAndEnabledTrue(Role.AUDITOR_ADMIN).size() <= 1) {
            auditService.record(adminUsername, "DISABLE_ACCOUNT", target, "DENIED");
            throw new AccessDeniedException("Not allowed");
        }

        user.disable();
        userRepository.save(user);
        auditService.record(adminUsername, "DISABLE_ACCOUNT", target, "SUCCESS");
    }

    private User requireAdmin(String username, String action) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Not found"));
        if (user.getRole() != Role.AUDITOR_ADMIN || !user.isEnabled()) {
            auditService.record(username, action, "admin", "DENIED");
            throw new AccessDeniedException("Not allowed");
        }
        return user;
    }
}