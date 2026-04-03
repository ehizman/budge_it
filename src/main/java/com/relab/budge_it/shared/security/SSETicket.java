package com.relab.budge_it.shared.security;

import com.relab.budge_it.shared.domain.RecordStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Table(schema = "security", name = "sse_tickets")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SSETicket {
    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID jobId;

    @NotNull
    @Column(name = "token", nullable = false, updatable = false)
    private String token;

    @NotNull
    @Column (name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @NotNull
    @Builder.Default
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private RecordStatus status = RecordStatus.ACTIVE;
}
