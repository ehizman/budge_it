package com.relab.budge_it.profile.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(schema = "profile", name = "financial_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialProfile {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    @Column(name = "age_range")
    private String ageRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status")
    private EmploymentStatus employmentStatus;

    @Column(name = "monthly_net_income", precision = 15, scale = 2)
    private BigDecimal monthlyNetIncome;

    // JSONB fields — stored as JSON in Postgres, mapped to Java objects
    /**
     * Monthly expense breakdown — maps to the monthlyBudget JSONB column.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB", name = "expenses")
    private Map<String, Object> monthlyBudget;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private List<Map<String, Object>> debt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private List<Map<String, Object>> investment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private List<String> goals;

    @Column(name = "monthly_savings_target", precision = 15, scale = 2)
    private BigDecimal monthlySavingsTarget;

    /**
     * Increments on every PUT — AdvisorOrchestrationService snapshots this
     * so we know which version of the profile the AI model actually saw.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    // ─── Enums ────────────────────────────────────────────────────────────────

    public enum EmploymentStatus {
        EMPLOYED_FULL_TIME, SELF_EMPLOYED, FREELANCE, STUDENT, UNEMPLOYED
    }
}