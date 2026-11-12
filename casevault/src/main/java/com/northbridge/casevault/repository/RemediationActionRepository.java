package com.northbridge.casevault.repository;

import com.northbridge.casevault.model.RemediationAction;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface RemediationActionRepository extends Repository<RemediationAction, Long> {

    RemediationAction save(RemediationAction action);

    List<RemediationAction> findAllByIncidentCaseIdOrderByCreatedAtAsc(Long caseId);
}