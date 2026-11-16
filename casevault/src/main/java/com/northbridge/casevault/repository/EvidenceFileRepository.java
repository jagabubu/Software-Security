package com.northbridge.casevault.repository;

import com.northbridge.casevault.model.EvidenceFile;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface EvidenceFileRepository extends Repository<EvidenceFile, Long> {

    EvidenceFile save(EvidenceFile file);

    List<EvidenceFile> findAllByIncidentCaseIdOrderByCreatedAtAsc(Long caseId);

    // Always looked up by evidence id AND case id, never by evidence id alone
    Optional<EvidenceFile> findByIdAndIncidentCaseId(Long id, Long caseId);

    long countByIncidentCaseId(Long caseId);
}