package com.relab.budge_it.identity.event;

import java.util.UUID;

public final class IdentityEvents {

    private IdentityEvents() {}

    /**
     * Published when a new user successfully registers.
     */
    public record UserRegisteredEvent(
            UUID userId,
            String email,
            String firstName,
            String lastName
    ) {}

    /**
     * Published when a user's password is reset.
     * USed for audit logging
     */
    public record PasswordResetEvent(
            UUID userId,
            String email
    ) {}
}