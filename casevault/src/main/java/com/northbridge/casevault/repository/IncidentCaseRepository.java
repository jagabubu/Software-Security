package com.northbridge.casevault.repository;

import com.northbridge.casevault.model.IncidentCase;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface IncidentCaseRepository extends Repository<IncidentCase, Long> {

    IncidentCase save(IncidentCase incidentCase);

    Optional<IncidentCase> findByIdAndOrganisationId(Long id, Long organisationId);

    List<IncidentCase> findAllByOrganisationIdOrderByCreatedAtDesc(Long organisationId);
}