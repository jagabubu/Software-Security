package com.northbridge.casevault.repository;

import com.northbridge.casevault.model.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganisationRepository extends JpaRepository<Organisation, Long> {
}