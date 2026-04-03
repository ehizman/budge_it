package com.relab.budge_it.profile.repository;

import com.relab.budge_it.profile.domain.FinancialProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialProfileRepository extends JpaRepository<FinancialProfile, UUID> {

    // One profile per user — used by the service to check if one already exists
    Optional<FinancialProfile> findByUserId(UUID userId);

    // Used by AdvisorOrchestrationService to check profile exists before generating
    boolean existsByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}