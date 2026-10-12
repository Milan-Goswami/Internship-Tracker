package com.interntrack.api.security;

import com.interntrack.api.entity.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // 512-bit test secret key (64 characters)
    private static final String TEST_SECRET = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";
    private static final long EXPIRATION_MS = 900_000L; // 15 minutes

    private JwtService jwtService;
    private AuthenticatedUser sampleUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS);
        sampleUser = new AuthenticatedUser(100L, "candidate@example.com", "Alice Candidate", UserRole.ROLE_USER, true);
    }

    @Test
    @DisplayName("generate: Valid user should create a non-blank, compact HS512 JWT")
    void generate_validUser_createsValidToken() {
        String token = jwtService.generateAccessToken(sampleUser);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // Header, Payload, Signature
    }

    @Test
    @DisplayName("claims: Generated token must contain required claims: sub, email, fullName, role, iat, exp")
    void generatedTokenContainsRequiredClaims() {
        String token = jwtService.generateAccessToken(sampleUser);

        Claims claims = jwtService.extractAllClaims(token);

        assertThat(claims.getSubject()).isEqualTo("100");
        assertThat(claims.get("email", String.class)).isEqualTo("candidate@example.com");
        assertThat(claims.get("fullName", String.class)).isEqualTo("Alice Candidate");
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_USER");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    @DisplayName("security: Generated token must never contain password, passwordHash, or sensitive secrets")
    void generatedTokenDoesNotContainPassword() {
        String token = jwtService.generateAccessToken(sampleUser);

        Claims claims = jwtService.extractAllClaims(token);

        assertThat(claims.keySet()).doesNotContain("password", "passwordHash", "rawPassword", "secret");
    }

    @Test
    @DisplayName("validation: Valid, unexpired token should validate successfully")
    void validToken_validatesSuccessfully() {
        String token = jwtService.generateAccessToken(sampleUser);

        boolean isValid = jwtService.validateAccessToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("validation: Expired token must be rejected by validateAccessToken and isTokenExpired")
    void expiredToken_isRejected() {
        // Create an already-expired token service (-10 seconds expiration)
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        JwtService expiredJwtService = new JwtService(key, -10_000L);
        String expiredToken = expiredJwtService.generateAccessToken(sampleUser);

        boolean isValid = jwtService.validateAccessToken(expiredToken);
        boolean isExpired = jwtService.isTokenExpired(expiredToken);

        assertThat(isValid).isFalse();
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("validation: Malformed or truncated token string must be rejected")
    void malformedToken_isRejected() {
        assertThat(jwtService.validateAccessToken("invalid.jwt.token")).isFalse();
        assertThat(jwtService.validateAccessToken("eyJhbGciOiJIUzUxMiJ9.e30.invalid")).isFalse();
    }

    @Test
    @DisplayName("validation: Token signed with a different secret key must be rejected")
    void invalidSignature_isRejected() {
        String otherSecret = "DIFFERENT_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";
        JwtService attackerJwtService = new JwtService(otherSecret, EXPIRATION_MS);
        String forgedToken = attackerJwtService.generateAccessToken(sampleUser);

        boolean isValid = jwtService.validateAccessToken(forgedToken);

        assertThat(isValid).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("validation: Null or blank token must return false gracefully")
    void blankToken_isRejected(String blankToken) {
        assertThat(jwtService.validateAccessToken(blankToken)).isFalse();
    }

    @Test
    @DisplayName("validation: Token missing required claims (e.g. missing role or invalid subject) must be rejected")
    void missingRequiredClaim_isRejected() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        // Token without role claim
        String incompleteToken = Jwts.builder()
                .subject("100")
                .claim("email", "candidate@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key, Jwts.SIG.HS512)
                .compact();

        assertThat(jwtService.validateAccessToken(incompleteToken)).isFalse();
    }

    @Test
    @DisplayName("validation: Token signed with unsupported algorithm (HS256 when HS512 expected) must be rejected")
    void unsupportedAlgorithm_isRejected() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String hs256Token = Jwts.builder()
                .subject("100")
                .claim("email", "candidate@example.com")
                .claim("role", "ROLE_USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key, Jwts.SIG.HS256) // Signed with HS256 instead of HS512
                .compact();

        // JJWT parser with verifyWith(key) will safely reject or match key algorithm
        assertThat(jwtService.validateAccessToken(hs256Token)).isFalse();
    }

    @Test
    @DisplayName("expiration: Token expiration timestamp must align with configured expiration duration")
    void tokenExpirationIsCorrect() {
        long beforeMs = System.currentTimeMillis();
        String token = jwtService.generateAccessToken(sampleUser);
        long afterMs = System.currentTimeMillis();

        Date expiration = jwtService.extractExpiration(token);

        assertThat(expiration.getTime()).isBetween(beforeMs + EXPIRATION_MS - 1000, afterMs + EXPIRATION_MS + 1000);
    }

    @Test
    @DisplayName("distinct: Different users must produce different tokens")
    void differentUsersProduceDifferentTokens() {
        AuthenticatedUser user1 = new AuthenticatedUser(1L, "u1@example.com", "U1", UserRole.ROLE_USER, true);
        AuthenticatedUser user2 = new AuthenticatedUser(2L, "u2@example.com", "U2", UserRole.ROLE_ADMIN, true);

        String token1 = jwtService.generateAccessToken(user1);
        String token2 = jwtService.generateAccessToken(user2);

        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtService.extractUserId(token1)).isEqualTo(1L);
        assertThat(jwtService.extractUserId(token2)).isEqualTo(2L);
        assertThat(jwtService.extractRole(token2)).isEqualTo(UserRole.ROLE_ADMIN);
    }

    @Test
    @DisplayName("principal: extractAuthenticatedUser correctly reconstructs user representation")
    void extractAuthenticatedUser_reconstructsPrincipal() {
        String token = jwtService.generateAccessToken(sampleUser);

        AuthenticatedUser reconstructed = jwtService.extractAuthenticatedUser(token);

        assertThat(reconstructed.id()).isEqualTo(100L);
        assertThat(reconstructed.email()).isEqualTo("candidate@example.com");
        assertThat(reconstructed.fullName()).isEqualTo("Alice Candidate");
        assertThat(reconstructed.role()).isEqualTo(UserRole.ROLE_USER);
        assertThat(reconstructed.isActive()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("security: Missing or blank production secret must throw IllegalStateException")
    void missingProductionSecret_throwsIllegalStateException(String missingSecret) {
        assertThatThrownBy(() -> new JwtService(missingSecret, EXPIRATION_MS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET_KEY is required");
    }

    @Test
    @DisplayName("security: Weak secret key (< 64 bytes) must throw IllegalArgumentException and not leak secret value")
    void weakSecretKey_throwsException() {
        String shortSecret = "too_short_key_under_64_bytes";

        assertThatThrownBy(() -> new JwtService(shortSecret, EXPIRATION_MS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 512 bits (64 bytes)")
                .hasMessageNotContaining(shortSecret);
    }

    @Test
    @DisplayName("security: Valid 64-byte secret successfully initializes and operates")
    void valid64ByteSecret_works() {
        JwtService validService = new JwtService(TEST_SECRET, EXPIRATION_MS);
        String token = validService.generateAccessToken(sampleUser);

        assertThat(validService.validateAccessToken(token)).isTrue();
    }

    @Test
    @DisplayName("testing: Programmatic constructor allows test-only SecretKey injection")
    void testCanExplicitlyProvideTestKey() {
        SecretKey mockKey = Jwts.SIG.HS512.key().build();
        JwtService testService = new JwtService(mockKey, 60_000L);

        String token = testService.generateAccessToken(sampleUser);
        assertThat(testService.validateAccessToken(token)).isTrue();
    }
}