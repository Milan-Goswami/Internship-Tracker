package com.interntrack.api.service;

import com.interntrack.api.entity.RefreshToken;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.InvalidTokenException;
import com.interntrack.api.repository.RefreshTokenRepository;
import com.interntrack.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration regression suite verifying DEF-SEC-01 remediation:
 * Proves that breach replay invalidation successfully commits all active tokens
 * across the user's token family to the database, ensuring concurrent sessions
 * are revoked while unrelated users remain unaffected.
 */
@SpringBootTest(properties = {
        "security.jwt.secret=FAMILY_REVOCATION_TEST_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class RefreshTokenFamilyRevocationIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        userA = new User("candidate-a@example.com", "$2a$12$hashA", "Candidate A", UserRole.ROLE_USER);
        userA.setIsActive(true);
        userA = userRepository.save(userA);

        userB = new User("candidate-b@example.com", "$2a$12$hashB", "Candidate B", UserRole.ROLE_USER);
        userB.setIsActive(true);
        userB = userRepository.save(userB);
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Scenario A: Normal token rotation succeeds and invalidates the old token single-use")
    void scenarioA_normalTokenRotation_succeeds() {
        // 1 & 2. Obtain RT1
        String rt1 = refreshTokenService.createRefreshToken(userA);
        assertThat(rt1).isNotBlank();

        // 3 & 4. Rotate RT1
        TokenRotationResult result = refreshTokenService.rotateRefreshToken(rt1);
        assertThat(result.user().getId()).isEqualTo(userA.getId());
        assertThat(result.rawRefreshToken()).isNotBlank();
        assertThat(result.rawRefreshToken()).isNotEqualTo(rt1);

        // 5. Verify RT1 is revoked in database
        String rt1Hash = refreshTokenService.hashToken(rt1);
        RefreshToken rt1Entity = refreshTokenRepository.findByTokenHash(rt1Hash).orElseThrow();
        assertThat(rt1Entity.getRevoked()).isTrue();
        assertThat(rt1Entity.getRevokedAt()).isNotNull();

        // Subsequent rotation with RT1 must fail
        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rt1))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("token reuse detected");
    }

    @Test
    @DisplayName("Scenario B: Replaying revoked token triggers persisted family revocation (DEF-SEC-01 Fix Verification)")
    void scenarioB_replayTriggersPersistedFamilyRevocation() {
        // 1. Authenticate User A in two independent sessions (RT1 and RT2)
        String rt1 = refreshTokenService.createRefreshToken(userA);
        String rt2 = refreshTokenService.createRefreshToken(userA);
        assertThat(rt1).isNotEqualTo(rt2);

        // Unrelated User B has an active session RT_B
        String rtB = refreshTokenService.createRefreshToken(userB);

        // 2. Rotate RT1 successfully -> RT1_new is issued; RT1 is now revoked
        TokenRotationResult rotateResult1 = refreshTokenService.rotateRefreshToken(rt1);
        String rt1New = rotateResult1.rawRefreshToken();

        // Verify pre-replay state: RT2 and RT1_new are active
        List<RefreshToken> activeBeforeReplay = refreshTokenRepository.findByUserIdAndRevokedFalse(userA.getId());
        assertThat(activeBeforeReplay).hasSize(2);

        // 3 & 4. Attacker replays old RT1 -> must throw InvalidTokenException
        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rt1))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Revoked refresh token presented: possible token reuse detected");

        // 5. CRITICAL DEF-SEC-01 CHECK: Verify RT2 and RT1_new are PERSISTENTLY REVOKED in the database
        List<RefreshToken> activeAfterReplay = refreshTokenRepository.findByUserIdAndRevokedFalse(userA.getId());
        assertThat(activeAfterReplay).as("All active tokens in User A's family must be revoked").isEmpty();

        String rt2Hash = refreshTokenService.hashToken(rt2);
        RefreshToken rt2Entity = refreshTokenRepository.findByTokenHash(rt2Hash).orElseThrow();
        assertThat(rt2Entity.getRevoked()).as("RT2 must be committed as revoked in database").isTrue();
        assertThat(rt2Entity.getRevokedAt()).isNotNull();

        String rt1NewHash = refreshTokenService.hashToken(rt1New);
        RefreshToken rt1NewEntity = refreshTokenRepository.findByTokenHash(rt1NewHash).orElseThrow();
        assertThat(rt1NewEntity.getRevoked()).as("RT1-new must be committed as revoked in database").isTrue();

        // 6. Attempt to use RT2 from Session 2 -> must be rejected
        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rt2))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Revoked refresh token presented");

        // Attempt to use RT1_new -> must also be rejected
        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rt1New))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Revoked refresh token presented");

        // 8. Verify unrelated User B's active refresh token is COMPLETELY UNAFFECTED
        String rtBHash = refreshTokenService.hashToken(rtB);
        RefreshToken rtBEntity = refreshTokenRepository.findByTokenHash(rtBHash).orElseThrow();
        assertThat(rtBEntity.getRevoked()).isFalse();

        TokenRotationResult rotateResultB = refreshTokenService.rotateRefreshToken(rtB);
        assertThat(rotateResultB.rawRefreshToken()).isNotBlank();
        assertThat(rotateResultB.user().getId()).isEqualTo(userB.getId());
    }

    @Test
    @DisplayName("Scenario C: Normal behavior after re-authentication following breach revocation")
    void scenarioC_normalBehaviorAfterReauthentication() {
        // Create RT1 and RT2, then trigger breach replay
        String rt1 = refreshTokenService.createRefreshToken(userA);
        String rt2 = refreshTokenService.createRefreshToken(userA);
        refreshTokenService.rotateRefreshToken(rt1);

        // Breach event
        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rt1))
                .isInstanceOf(InvalidTokenException.class);

        // Both previous sessions are now dead
        assertThat(refreshTokenRepository.findByUserIdAndRevokedFalse(userA.getId())).isEmpty();

        // User A logs back in with valid credentials
        String rt3 = refreshTokenService.createRefreshToken(userA);
        assertThat(rt3).isNotBlank();

        // RT3 is active and rotates normally
        TokenRotationResult rotateResult3 = refreshTokenService.rotateRefreshToken(rt3);
        assertThat(rotateResult3.rawRefreshToken()).isNotBlank();
        assertThat(rotateResult3.rawRefreshToken()).isNotEqualTo(rt3);

        String rt3Hash = refreshTokenService.hashToken(rt3);
        RefreshToken rt3Entity = refreshTokenRepository.findByTokenHash(rt3Hash).orElseThrow();
        assertThat(rt3Entity.getRevoked()).isTrue();
    }

    @Test
    @DisplayName("Scenario D: Unknown token rejection does not affect active user tokens")
    void scenarioD_unknownToken_doesNotAffectActiveTokens() {
        String activeToken = refreshTokenService.createRefreshToken(userA);

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("completely-unknown-token-xyz"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid refresh token");

        // User A's token remains active
        String activeHash = refreshTokenService.hashToken(activeToken);
        RefreshToken entity = refreshTokenRepository.findByTokenHash(activeHash).orElseThrow();
        assertThat(entity.getRevoked()).isFalse();
    }
}
