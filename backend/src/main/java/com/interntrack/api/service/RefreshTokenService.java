package com.interntrack.api.service;

import com.interntrack.api.entity.RefreshToken;
import com.interntrack.api.entity.User;
import com.interntrack.api.exception.AccountInactiveException;
import com.interntrack.api.exception.InvalidTokenException;
import com.interntrack.api.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * Service managing refresh token lifecycle, SHA-256 token hashing,
 * rotation, and reuse breach detection.
 */
@Service
@Transactional
public class RefreshTokenService {

    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Generates and stores a new cryptographically secure refresh token for the user.
     *
     * @param user the user for whom the token is created
     * @return the raw refresh token string to be returned to the client
     */
    public String createRefreshToken(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AccountInactiveException("Cannot create refresh token for inactive account");
        }

        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS);

        RefreshToken refreshToken = new RefreshToken(user, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    /**
     * Validates and rotates an incoming refresh token using strict single-use semantics
     * with automated breach invalidation on token reuse.
     *
     * @param rawRefreshToken the presenting raw refresh token string
     * @return the rotation result containing the authenticated User and new raw refresh token
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public TokenRotationResult rotateRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }

        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        // Reuse detection: if token is already revoked, invalidate all active tokens for this user
        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            revokeAllUserTokens(refreshToken.getUser().getId());
            throw new InvalidTokenException("Revoked refresh token presented: possible token reuse detected");
        }

        // Expiration check
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        // Inactive account check
        if (!Boolean.TRUE.equals(refreshToken.getUser().getIsActive())) {
            throw new AccountInactiveException("User account is inactive");
        }

        // Issue new token
        String newRawToken = generateRawToken();
        String newHash = hashToken(newRawToken);
        Instant newExpiresAt = Instant.now().plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS);

        RefreshToken newRefreshToken = new RefreshToken(refreshToken.getUser(), newHash, newExpiresAt);
        refreshTokenRepository.save(newRefreshToken);

        // Mark current token as revoked and linked
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(Instant.now());
        refreshToken.setReplacedByTokenHash(newHash);
        refreshTokenRepository.save(refreshToken);

        return new TokenRotationResult(refreshToken.getUser(), newRawToken);
    }

    /**
     * Revokes a single refresh token by its raw string value.
     *
     * @param rawRefreshToken the raw refresh token string to revoke
     */
    public void revokeRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHashForUpdate(tokenHash).ifPresent(token -> {
            if (!Boolean.TRUE.equals(token.getRevoked())) {
                token.setRevoked(true);
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    /**
     * Revokes all active tokens for a specific user ID upon breach detection or logout.
     *
     * @param userId the ID of the user whose tokens should be revoked
     */
    public void revokeAllUserTokens(Long userId) {
        if (userId == null) {
            return;
        }
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        Instant now = Instant.now();
        for (RefreshToken token : activeTokens) {
            token.setRevoked(true);
            token.setRevokedAt(now);
        }
        refreshTokenRepository.saveAllAndFlush(activeTokens);
    }

    /**
     * Computes deterministic SHA-256 hex digest of a raw token.
     */
    public String hashToken(String rawToken) {
        if (rawToken == null) {
            throw new IllegalArgumentException("Token string cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[32]; // 256 bits of entropy
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}