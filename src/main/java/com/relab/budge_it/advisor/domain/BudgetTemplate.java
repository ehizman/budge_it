package com.relab.budge_it.advisor.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
@Getter
@Setter
@Entity
@Table(name = "budget_templates", schema = "advisor", indexes = {
        @Index(name = "idx_budget_templates_job_id", columnList = "job_id"),
        @Index(name = "idx_budget_templates_user_id", columnList = "user_id, createdAt")
})
@AllArgsConstructor
@NoArgsConstructor
public class BudgetTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false, updatable = false)
    private AIJob job;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = Integer.MAX_VALUE, columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(name = "pocket_config", nullable = false, columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String pocketConfig;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "applied", nullable = false)
    @Builder.Default
    private Boolean applied = false;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "saved", nullable = false)
    @Builder.Default
    private Boolean saved = false;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getJobId() {
        return job.getId();
    }
}