package com.northbridge.casevault.service;

import com.northbridge.casevault.model.AuditEvent;
import com.northbridge.casevault.repository.AuditEventRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void record(String username, String action, String target, String outcome) {
        auditEventRepository.save(new AuditEvent(username, action, target, outcome));
    }

    public long countEvents() {
        return auditEventRepository.count();
    }
}