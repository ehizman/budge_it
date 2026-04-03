package com.relab.budge_it.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class IdentityDtos {

    private IdentityDtos() {}

    // ─── Incoming ─────────────────────────────────────────────────────────────

    public record RegisterRequest(

            @NotBlank(message = "First name is required")
            String firstName,

            @NotBlank(message = "Last name is required")
            String lastName,

            @NotBlank(message = "Email is required")
            @Email(message = "Must be a valid email address")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password,

            String phone
    ) {}

    public record LoginRequest(

            @NotBlank(message = "Email is required")
            @Email(message = "Must be a valid email address")
            String email,

            @NotBlank(message = "Password is required")
            String password,

            // Optional — sent by the client to enable per-device session management
            String deviceFingerprint
    ) {}

    public record RefreshRequest(

            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}

    public record ChangePasswordRequest(

            @NotBlank(message = "Current password is required")
            String currentPassword,

            @NotBlank(message = "New password is required")
            @Size(min = 8, message = "New password must be at least 8 characters")
            String newPassword
    ) {}

    // ─── Outgoing ─────────────────────────────────────────────────────────────

    public record RegisterResponse(
            UUID userId,
            String firstName,
            String lastName,
            String email,
            String accountNumber,
            String bank,
            Instant createdAt
    ) {}

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            long expiresIn        // seconds until access token expires
    ) {}

    public record UserProfileResponse(
            UUID userId,
            String firstName,
            String lastName,
            String email,
            String accountNumber,
            String status,
            Instant createdAt
    ) {}
}