package com.relab.budge_it.profile.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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

            String monthlyNetIncome,

            Map<String, Object> monthlyBudget,

            List<Map<String, Object>> debt,

            List<Map<String, Object>> investment,

            List<String> goals,

            String monthlySavingsTarget
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
            String monthlyNetIncome,
            Map<String, Object> monthlyBudget,
            List<Map<String, Object>> debt,
            List<Map<String, Object>> investment,
            List<String> goals,
            String monthlySavingsTarget,
            int version,
            Instant createdAt,
            Instant updatedAt
    ) {}
}