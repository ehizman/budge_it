package com.relab.budge_it.profile.event;

import java.util.UUID;

public final class ProfileEvents {

    private ProfileEvents() {}

    /**
     * Published when a user creates or updates their financial profile.
     * Currently consumed by nothing — reserved for future use e.g.
     * triggering a nudge to generate a budget if the user hasn't yet.
     */
    public record FinancialProfileUpdatedEvent(
            UUID userId,
            int newVersion
    ) {}
}