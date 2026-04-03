package com.relab.budge_it.profile.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
     * Monthly expense breakdown — maps to the expenses JSONB column.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private String expenses;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private List<DebtInfo> debt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private List<InvestmentInfo> investment;

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

    public enum PaymentFrequency {
        MONTHLY("monthly"),
        QUARTERLY("quarterly"),
        ANNUALLY("annually"),
        CUSTOM("custom"); // For irregular payment schedules

        public final String frequency;

        PaymentFrequency(String frequency) {
            this.frequency = frequency;
        }
    }

    // ─── Nested JSONB types ───────────────────────────────────────────────────
    /**
     * Debt information — maps to the debt JSONB column.
     */
    public record DebtInfo(
            Boolean hasDebt,
            PaymentFrequency paymentFrequency,
            BigDecimal amount,
            List<String> types      // e.g. ["STUDENT_LOAN", "CREDIT_CARD"]
    ) {}

    /**
     * Investment information — maps to the investment JSONB column.
     */
    public record InvestmentInfo(
            Boolean invests,
            PaymentFrequency paymentFrequency,
            BigDecimal amount,
            List<String> types      // e.g. ["STOCKS", "CRYPTO", "REAL_ESTATE"]
    ) {}
}