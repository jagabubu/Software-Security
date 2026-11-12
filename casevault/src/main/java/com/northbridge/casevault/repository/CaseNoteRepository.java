package com.northbridge.casevault.repository;

import com.northbridge.casevault.model.CaseNote;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface CaseNoteRepository extends Repository<CaseNote, Long> {

    CaseNote save(CaseNote note);

    List<CaseNote> findAllByIncidentCaseIdOrderByCreatedAtAsc(Long caseId);
}