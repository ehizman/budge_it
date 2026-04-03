package com.relab.budge_it.advisor.repository;

import com.relab.budge_it.advisor.domain.AIJob.JobStatus;
import com.relab.budge_it.advisor.domain.AIJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AIJobRepository extends JpaRepository<AIJob, UUID> {
    Optional<AIJob> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, JobStatus status);
    List<AIJob> findByStatusAndCreatedAtBefore(JobStatus status, Instant cutoff);
    Optional<AIJob> findByUserIdAndStatus(UUID userId, JobStatus jobStatus);
}
