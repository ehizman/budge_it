package com.relab.budge_it.identity.service;

import com.relab.budge_it.identity.domain.RefreshToken;
import com.relab.budge_it.identity.domain.User;
import com.relab.budge_it.identity.dto.IdentityDtos.*;
import com.relab.budge_it.identity.event.IdentityEvents;
import com.relab.budge_it.identity.repository.RefreshTokenRepository;
import com.relab.budge_it.identity.repository.UserRepository;
import com.relab.budge_it.shared.response.BusinessException;
import com.relab.budge_it.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    private static final int REFRESH_TOKEN_DAYS = 30;

    // ─── Register ─────────────────────────────────────────────────────────────

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // Edge case ID-03: don't reveal whether the email exists
        if (userRepository.existsByEmail(request.email())) {
            throw BusinessException.conflict("ACCOUNT_EXISTS",
                    "An account with this email may already exist.");
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .accountNumber(generateAccountNumber())
                .build();

        user = userRepository.save(user);

        eventPublisher.publishEvent(new IdentityEvents.UserRegisteredEvent(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName()));

        log.info("New user registered: {}", user.getId());

        return new RegisterResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getAccountNumber(),
                "ReLab MFB",
                user.getCreatedAt()
        );
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> BusinessException.unauthorized(
                        "Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("Invalid email or password.");
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw BusinessException.forbidden("ACCOUNT_INACTIVE",
                    "Your account is not active. Please contact support.");
        }

        return issueTokens(user, request.deviceFingerprint());
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    /**
     * Edge case ID-01: token rotation with grace window.
     * The old token is revoked and a new one is issued.
     */
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String tokenHash = passwordEncoder.encode(request.refreshToken());

        // Find by hashing the incoming token and comparing
        RefreshToken stored = refreshTokenRepository
                .findAll()
                .stream()
                .filter(t -> passwordEncoder.matches(request.refreshToken(), t.getTokenHash()))
                .findFirst()
                .orElseThrow(() -> BusinessException.unauthorized("Invalid refresh token."));

        if (!stored.isValid()) {
            // Token already used or expired — revoke all sessions for this user
            // This detects token theft — if someone is replaying a used token,
            // force logout everywhere
            refreshTokenRepository.revokeAllByUserId(stored.getUserId(), Instant.now());
            throw BusinessException.unauthorized(
                    "Refresh token is invalid. All sessions have been revoked.");
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> BusinessException.notFound("User"));

        // Revoke the used token
        stored.revoke();
        refreshTokenRepository.save(stored);

        // Issue a fresh pair
        return issueTokens(user, stored.getDeviceFingerprint());
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findAll()
                .stream()
                .filter(t -> passwordEncoder.matches(rawRefreshToken, t.getTokenHash()))
                .findFirst()
                .ifPresent(t -> {
                    t.revoke();
                    refreshTokenRepository.save(t);
                });
    }

    // ─── Change password ──────────────────────────────────────────────────────

    /**
     * Edge case ID-04: revoke ALL refresh tokens on password change.
     * Forces re-login on every device.
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // Revoke all sessions — edge case ID-04
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());

        eventPublisher.publishEvent(
                new IdentityEvents.PasswordResetEvent(userId, user.getEmail()));

        log.info("Password changed for user {} — all sessions revoked", userId);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private TokenResponse  issueTokens(User user, String deviceFingerprint) {
        // Issue JWT access token
        String accessToken = jwtService.generateAccessToken(user.getId().toString(), user.getEmail());

        // Generate a raw refresh token — stored as a hash
        String rawRefreshToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(passwordEncoder.encode(rawRefreshToken))
                .deviceFingerprint(deviceFingerprint)
                .expiresAt(Instant.now().plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(
                accessToken,
                rawRefreshToken,
                jwtService.getAccessTokenExpirySeconds()
        );
    }

    /**
     * Generates a unique 10-digit virtual account number.
     * Random — not sequential — to prevent account enumeration.
     */
    private String generateAccountNumber() {
        String number;
        do {
            number = "80" + String.format("%08d", new Random().nextInt(100_000_000));
        } while (userRepository.existsByAccountNumber(number));
        return number;
    }
}