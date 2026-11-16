package com.northbridge.casevault.repository;

import com.northbridge.casevault.model.IncidentCase;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface IncidentCaseRepository extends Repository<IncidentCase, Long> {

    IncidentCase save(IncidentCase incidentCase);

    // Only called by the service AFTER a role check (case managers see all cases)
    Optional<IncidentCase> findById(Long id);

    Optional<IncidentCase> findByIdAndOrganisationId(Long id, Long organisationId);

    Optional<IncidentCase> findByIdAndOwnerId(Long id, Long ownerId);

    List<IncidentCase> findAllByOrderByCreatedAtDesc();

    List<IncidentCase> findAllByOrganisationIdOrderByCreatedAtDesc(Long organisationId);

    List<IncidentCase> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}