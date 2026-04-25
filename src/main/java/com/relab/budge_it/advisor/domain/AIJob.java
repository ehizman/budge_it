package com.relab.budge_it.advisor.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
@Setter
@Entity
@Table(name = "ai_jobs", schema = "advisor", indexes = {
        @Index(name = "idx_ai_jobs_user_status", columnList = "user_id, status"),
        @Index(name = "idx_ai_jobs_status_created_at", columnList = "status, created_at")
})
@NoArgsConstructor
@AllArgsConstructor
public class AIJob {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "profile_version", nullable = false)
    private Integer profileVersion;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING'")
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", length = Integer.MAX_VALUE, columnDefinition = "TEXT")
    private String errorMessage;

    @NotNull
    @ColumnDefault("now()")
    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    public enum JobStatus {
        PENDING, COMPLETED, FAILED
    }
}