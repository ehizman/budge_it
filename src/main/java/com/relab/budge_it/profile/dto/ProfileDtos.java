package com.relab.budge_it.profile.dto;

import com.relab.budge_it.profile.domain.FinancialProfile.DebtInfo;
import com.relab.budge_it.profile.domain.FinancialProfile.InvestmentInfo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProfileDtos {

    private ProfileDtos() {}

    // ─── Incoming ─────────────────────────────────────────────────────────────

    /**
     * Request body for PUT /users/me/financial-profile.
     * Every field is optional except monthlyNetIncome — THe AI model needs that
     * to generate meaningful budget allocations.
     */
    public record UpsertProfileRequest(

            String ageRange,

            String employmentStatus,

            @NotNull(message = "Monthly net income is required")
            @DecimalMin(value = "0.0", inclusive = false, message = "Income must be greater than zero")
            BigDecimal monthlyNetIncome,

            String expenses,

            List<DebtInfo> debt,

            List<InvestmentInfo> investment,

            List<String> goals,

            BigDecimal monthlySavingsTarget
    ) {}

    // ─── Outgoing ─────────────────────────────────────────────────────────────

    /**
     * Returned on GET and PUT /users/me/financial-profile.
     */
    public record ProfileResponse(
            UUID id,
            UUID userId,
            String ageRange,
            String employmentStatus,
            BigDecimal monthlyNetIncome,
            String expenses,
            List<DebtInfo> debt,
            List<InvestmentInfo> investment,
            List<String> goals,
            BigDecimal monthlySavingsTarget,
            int version,
            Instant createdAt,
            Instant updatedAt
    ) {}
}