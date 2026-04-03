package com.relab.budge_it.advisor.event;

import java.util.List;
import java.util.UUID;

public final class AdvisorEvents {

    // private AdvisorEvents() {} — prevents anyone from instantiating the outer class.
    // It's just a namespace, not an object.
    private AdvisorEvents() {}

    /**
     * Published when a user submits their financial profile for AI generation.
     * Consumed by AdvisorOrchestrationService to trigger the async AI Model call.
     *
     * @param jobId          the AIJob that was created for this request
     * @param userId         the user who requested generation
     * @param profileVersion snapshot of which profile version was sent to the AI Model
     */
    public record FinancialProfileSubmittedEvent(
            UUID jobId,
            UUID userId,
            int profileVersion
    ) {}

    /**
     * Published when AI Model successfully returns at most 5 budget templates.
     * Consumed by NotificationService to alert the user.
     *
     * @param jobId       the completed AIJob
     * @param userId      the user who requested generation
     * @param templateIds the BudgetTemplate IDs that were saved
     */
    public record BudgetTemplatesGeneratedEvent(
            UUID jobId,
            UUID userId,
            List<UUID> templateIds
    ) {}

    /**
     * Published when an AI job fails after all retries are exhausted.
     * Consumed by NotificationService to alert the user.
     *
     * @param jobId     the failed AIJob
     * @param userId    the user who requested generation
     * @param errorCode machine-readable failure reason e.g. AI_INVALID_RESPONSE
     */
    public record AIJobFailedEvent(
            UUID jobId,
            UUID userId,
            String errorCode
    ) {}
}