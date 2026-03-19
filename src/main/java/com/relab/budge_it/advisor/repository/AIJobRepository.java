package com.relab.budgetpro.advisor.repository;

import com.relab.budgetpro.advisor.domain.AIJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AIJobRepository extends JpaRepository<AIJob, UUID> {
    Optional<AIJob> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, AIJob.JobStatus status);
    List<AIJob> findByStatusAndCreatedAtBefore(AIJob.JobStatus status, Instant cutoff);
}
