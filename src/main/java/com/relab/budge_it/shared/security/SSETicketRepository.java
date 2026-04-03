package com.relab.budge_it.shared.security;

import com.relab.budge_it.shared.domain.RecordStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SSETicketRepository extends JpaRepository<SSETicket, UUID> {
    @Query("SELECT se FROM SSETicket se where se.jobId = :jobId AND se.status = 'ACTIVE'")
    Optional<SSETicket> findFirstByJobIdAndStatusIsActive(@Param("jobId") UUID jobId);
}
