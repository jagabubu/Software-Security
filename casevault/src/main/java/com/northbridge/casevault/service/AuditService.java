package com.northbridge.casevault.service;

import com.northbridge.casevault.model.AuditEvent;
import com.northbridge.casevault.repository.AuditEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void record(String username, String action, String target, String outcome) {
        auditEventRepository.save(new AuditEvent(
                clean(username, 100), clean(action, 50), clean(target, 100), clean(outcome, 20)));
    }

    public long countEvents() {
        return auditEventRepository.count();
    }

    public List<AuditEvent> recentEvents() {
        return auditEventRepository.findTop100ByOrderByEventTimeDesc();
    }

    // Usernames typed on the login form are attacker-controlled. Strip control
    // characters (log injection) and cap the length (so the database can't be overflowed).
    private String clean(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("\\p{Cntrl}", " ");
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }
}