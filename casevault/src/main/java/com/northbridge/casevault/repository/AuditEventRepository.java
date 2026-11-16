package com.northbridge.casevault.repository;

import com.northbridge.casevault.model.AuditEvent;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface AuditEventRepository extends Repository<AuditEvent, Long> {

    AuditEvent save(AuditEvent event);

    List<AuditEvent> findAllByOrderByEventTimeDesc();

    List<AuditEvent> findTop100ByOrderByEventTimeDesc();

    long count();
}