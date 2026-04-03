package com.relab.budge_it.advisor.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AdvisorDtos {

    private AdvisorDtos() {}

    // ─── Incoming ────────────────────────────────────────────────────────────

    /**
     * Request body for POST /advisor/generate.
     * Empty — the user's financial profile is loaded server-side from their account.
     * We don't accept profile data here to prevent tampering.
     */
    public record GenerateRequest() {}

    // ─── Outgoing ────────────────────────────────────────────────────────────

    /**
     * Returned immediately on POST /advisor/generate (202 Accepted).
     * The client uses jobId to poll for completion.
     */
    public record GenerateResponse(
            UUID jobId,
            String status,
            String sseTicket,
            String message
    ) {}

    /**
     * Returned when the client polls GET /advisor/jobs/{jobId}.
     * Tells them whether the AI Model is still working, succeeded, or failed.
     */
    public record JobStatusResponse(
            UUID jobId,
            String status,
            String errorCode,
            String errorMessage,
            Instant createdAt,
            Instant completedAt
    ) {}

    /**
     * Returned in GET /advisor/templates — the full list of generated templates.
     * Each template contains the pocket breakdown the AI Model suggested.
     */
    public record TemplateResponse(
            UUID id,
            UUID jobId,
            String name,
            String description,
            List<PocketDefinition> pockets,
            boolean applied,
            boolean saved,
            Instant createdAt
    ) {}

    /**
     * A single pocket definition inside a template.
     * Represents one row of the AI Model's suggested pocket configuration.
     */
    public record PocketDefinition(
            String name,
            String type,
            BigDecimal suggestedAmount,
            Float percentage,
            String icon,
            String color,
            Integer priority,
            Boolean isDefault
    ) {
        // Returns a copy of this pocket with a new suggestedAmount
        public PocketDefinition withSuggestedAmount(BigDecimal newAmount) {
            return new PocketDefinition(
                    name, type, newAmount, percentage, icon, color, priority, isDefault
            );
        }
    }
}