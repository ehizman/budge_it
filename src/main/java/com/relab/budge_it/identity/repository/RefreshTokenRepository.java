package com.relab.budge_it.identity.repository;

import com.relab.budge_it.identity.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // Used during token refresh — find the token by its hash
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Find all active sessions for a user — used to check concurrent sessions
    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);

    // Find session by device — used for per-device logout
    Optional<RefreshToken> findByUserIdAndDeviceFingerprint(UUID userId, String deviceFingerprint);

    // Revoke ALL tokens for a user — called on password reset (edge case ID-04)
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true, r.revokedAt = :now WHERE r.userId = :userId")
    void revokeAllByUserId(UUID userId, Instant now);

    // Cleanup job — delete expired and revoked tokens older than 7 days
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.revoked = true AND r.revokedAt < :cutoff")
    void deleteRevokedBefore(Instant cutoff);
}