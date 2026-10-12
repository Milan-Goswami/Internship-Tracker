package com.interntrack.api.service;

import com.interntrack.api.entity.RefreshToken;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.AccountInactiveException;
import com.interntrack.api.exception.InvalidTokenException;
import com.interntrack.api.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        activeUser = new User("candidate@example.com", "hashed_pwd", "Alice Candidate", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(activeUser, "id", 42L);
        activeUser.setIsActive(true);

        inactiveUser = new User("inactive@example.com", "hashed_pwd", "Bob Inactive", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(inactiveUser, "id", 99L);
        inactiveUser.setIsActive(false);
    }

    @Test
    @DisplayName("Create refresh token generates high-entropy raw token, hashes it, and persists entity")
    void createRefreshToken_success() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        String rawToken = refreshTokenService.createRefreshToken(activeUser);

        assertThat(rawToken).isNotBlank();
        assertThat(rawToken.length()).isGreaterThanOrEqualTo(32);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(activeUser);
        assertThat(saved.getTokenHash()).isEqualTo(refreshTokenService.hashToken(rawToken));
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken); // Raw token never saved directly
        assertThat(saved.getRevoked()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(6, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("Create refresh token for inactive account throws AccountInactiveException")
    void createRefreshToken_inactiveAccount_throwsException() {
        assertThatThrownBy(() -> refreshTokenService.createRefreshToken(inactiveUser))
                .isInstanceOf(AccountInactiveException.class)
                .hasMessageContaining("inactive account");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rotate refresh token successfully revokes old token, links replacement hash, and returns new token")
    void rotateRefreshToken_success() {
        String oldRawToken = "valid-raw-refresh-token-12345678901234567890";
        String oldHash = refreshTokenService.hashToken(oldRawToken);

        RefreshToken oldEntity = new RefreshToken(activeUser, oldHash, Instant.now().plus(7, ChronoUnit.DAYS));
        oldEntity.setRevoked(false);

        when(refreshTokenRepository.findByTokenHashForUpdate(oldHash)).thenReturn(Optional.of(oldEntity));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        TokenRotationResult result = refreshTokenService.rotateRefreshToken(oldRawToken);

        assertThat(result.user()).isEqualTo(activeUser);
        assertThat(result.rawRefreshToken()).isNotBlank();
        assertThat(result.rawRefreshToken()).isNotEqualTo(oldRawToken);

        // Old entity must be revoked and linked
        assertThat(oldEntity.getRevoked()).isTrue();
        assertThat(oldEntity.getRevokedAt()).isNotNull();
        assertThat(oldEntity.getReplacedByTokenHash()).isEqualTo(refreshTokenService.hashToken(result.rawRefreshToken()));

        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Rotate refresh token with unknown token throws InvalidTokenException")
    void rotateRefreshToken_unknownToken_throwsException() {
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("unknown-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    @DisplayName("Rotate expired refresh token throws InvalidTokenException")
    void rotateRefreshToken_expiredToken_throwsException() {
        String rawToken = "expired-token";
        String hash = refreshTokenService.hashToken(rawToken);

        RefreshToken expiredEntity = new RefreshToken(activeUser, hash, Instant.now().minus(1, ChronoUnit.HOURS));
        expiredEntity.setRevoked(false);

        when(refreshTokenRepository.findByTokenHashForUpdate(hash)).thenReturn(Optional.of(expiredEntity));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Reuse detection: presenting revoked refresh token revokes all active tokens for user (automated breach invalidation)")
    void rotateRefreshToken_alreadyRevoked_triggersBreachInvalidation() {
        String rawToken = "reused-revoked-token";
        String hash = refreshTokenService.hashToken(rawToken);

        RefreshToken revokedEntity = new RefreshToken(activeUser, hash, Instant.now().plus(5, ChronoUnit.DAYS));
        revokedEntity.setRevoked(true);

        RefreshToken otherActiveToken = new RefreshToken(activeUser, "other-hash", Instant.now().plus(5, ChronoUnit.DAYS));
        otherActiveToken.setRevoked(false);

        when(refreshTokenRepository.findByTokenHashForUpdate(hash)).thenReturn(Optional.of(revokedEntity));
        when(refreshTokenRepository.findByUserIdAndRevokedFalse(42L)).thenReturn(List.of(otherActiveToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("token reuse detected");

        // Verify breach defense: all user's active tokens revoked
        assertThat(otherActiveToken.getRevoked()).isTrue();
        assertThat(otherActiveToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).saveAllAndFlush(List.of(otherActiveToken));
    }

    @Test
    @DisplayName("Rotate refresh token for deactivated user throws AccountInactiveException")
    void rotateRefreshToken_inactiveUser_throwsException() {
        String rawToken = "valid-token-for-inactive-user";
        String hash = refreshTokenService.hashToken(rawToken);

        RefreshToken entity = new RefreshToken(inactiveUser, hash, Instant.now().plus(5, ChronoUnit.DAYS));
        entity.setRevoked(false);

        when(refreshTokenRepository.findByTokenHashForUpdate(hash)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawToken))
                .isInstanceOf(AccountInactiveException.class)
                .hasMessageContaining("inactive");
    }
    @Test
    @DisplayName("Revoke refresh token marks token as revoked and persists")
    void revokeRefreshToken_success() {
        String rawToken = "token-to-revoke";
        String hash = refreshTokenService.hashToken(rawToken);

        RefreshToken entity = new RefreshToken(activeUser, hash, Instant.now().plus(5, ChronoUnit.DAYS));
        entity.setRevoked(false);

        when(refreshTokenRepository.findByTokenHashForUpdate(hash)).thenReturn(Optional.of(entity));

        refreshTokenService.revokeRefreshToken(rawToken);

        assertThat(entity.getRevoked()).isTrue();
        assertThat(entity.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(entity);
    }

    @Test
    @DisplayName("Revoke refresh token with null or blank does nothing")
    void revokeRefreshToken_nullOrBlank_noop() {
        refreshTokenService.revokeRefreshToken(null);
        refreshTokenService.revokeRefreshToken("   ");

        verify(refreshTokenRepository, never()).findByTokenHashForUpdate(any());
        verify(refreshTokenRepository, never()).save(any());
    }
}