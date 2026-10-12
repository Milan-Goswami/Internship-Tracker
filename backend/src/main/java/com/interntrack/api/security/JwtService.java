package com.interntrack.api.security;

import com.interntrack.api.entity.enums.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Service managing JWT Access Token generation, parsing, cryptographic signature verification,
 * and claims validation strictly using HMAC-SHA512 (HS512).
 *
 * Fails fast at startup if JWT_SECRET_KEY is missing, blank, or shorter than 64 bytes (512 bits).
 */
@Service
public class JwtService {

    public static final long DEFAULT_ACCESS_TOKEN_EXPIRATION_MS = 900_000L; // 15 Minutes
    private static final int MIN_SECRET_KEY_BITS = 512;
    private static final int MIN_SECRET_KEY_BYTES = MIN_SECRET_KEY_BITS / 8; // 64 Bytes
    private static final String REQUIRED_ALGORITHM = "HS512";

    private final SecretKey secretKey;
    private final long expirationMs;

    @Autowired
    public JwtService(
            @Value("${security.jwt.secret:}") String rawSecret,
            @Value("${security.jwt.access-token-expiration-ms:900000}") long expirationMs) {

        this.expirationMs = expirationMs > 0 ? expirationMs : DEFAULT_ACCESS_TOKEN_EXPIRATION_MS;

        if (rawSecret == null || rawSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET_KEY is required and must be configured via environment variable (JWT_SECRET_KEY) " +
                    "or application configuration (security.jwt.secret). It must be at least 512 bits (64 bytes) for HMAC-SHA512."
            );
        }

        byte[] secretBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_KEY_BYTES) {
            throw new IllegalArgumentException(
                    "JWT_SECRET_KEY must be at least " + MIN_SECRET_KEY_BITS + " bits (64 bytes) for HMAC-SHA512. " +
                    "Provided key length is " + (secretBytes.length * 8) + " bits (" + secretBytes.length + " bytes)."
            );
        }
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
    }

    /**
     * Constructor for explicit programmatic wiring and unit testing.
     */
    public JwtService(SecretKey secretKey, long expirationMs) {
        if (secretKey == null) {
            throw new IllegalArgumentException("SecretKey must not be null");
        }
        this.secretKey = secretKey;
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a compact HS512 JWT access token for an authenticated user principal.
     *
     * @param user authenticated user record
     * @return signed compact JWT string
     */
    public String generateAccessToken(AuthenticatedUser user) {
        if (user == null || user.id() == null || user.email() == null || user.role() == null) {
            throw new IllegalArgumentException("AuthenticatedUser and required principal claims must not be null");
        }

        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(user.id().toString())
                .claim("email", user.email())
                .claim("fullName", user.fullName())
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Validates whether a token is structurally valid, correctly signed with HS512, unexpired,
     * and contains all required claims.
     *
     * @param token compact JWT string
     * @return true if valid and authentic, false otherwise
     */
    public boolean validateAccessToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Claims claims = extractAllClaims(token);
            return isValidClaimsStructure(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extracts all claims from a signed JWT string after cryptographic signature verification
     * and strict algorithm verification.
     */
    public Claims extractAllClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT token must not be null or blank");
        }
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);

        String alg = jws.getHeader().getAlgorithm();
        if (!REQUIRED_ALGORITHM.equalsIgnoreCase(alg)) {
            throw new UnsupportedJwtException("Unsupported or disallowed algorithm in token header: " + alg);
        }

        return jws.getPayload();
    }

    /**
     * Extracts the User ID from the JWT subject claim.
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new MalformedJwtException("Missing subject (userId) in JWT claims");
        }
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            throw new MalformedJwtException("Invalid subject format in JWT claims: " + subject, e);
        }
    }

    /**
     * Extracts the email claim.
     */
    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        String email = claims.get("email", String.class);
        if (email == null || email.isBlank()) {
            throw new MalformedJwtException("Missing email claim in JWT");
        }
        return email;
    }

    /**
     * Extracts the UserRole claim.
     */
    public UserRole extractRole(String token) {
        Claims claims = extractAllClaims(token);
        String roleStr = claims.get("role", String.class);
        if (roleStr == null || roleStr.isBlank()) {
            throw new MalformedJwtException("Missing role claim in JWT");
        }
        try {
            return UserRole.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            throw new MalformedJwtException("Invalid role claim in JWT: " + roleStr, e);
        }
    }

    /**
     * Extracts the token expiration timestamp.
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Extracts the AuthenticatedUser principal representation from a validated JWT.
     */
    public AuthenticatedUser extractAuthenticatedUser(String token) {
        Claims claims = extractAllClaims(token);
        if (!isValidClaimsStructure(claims)) {
            throw new MalformedJwtException("JWT claims structure is incomplete or invalid");
        }

        Long userId = Long.parseLong(claims.getSubject());
        String email = claims.get("email", String.class);
        String fullName = claims.get("fullName", String.class);
        UserRole role = UserRole.valueOf(claims.get("role", String.class));

        return new AuthenticatedUser(userId, email, fullName, role, true);
    }

    /**
     * Checks if the token has expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private boolean isValidClaimsStructure(Claims claims) {
        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            return false;
        }
        try {
            Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            return false;
        }
        String email = claims.get("email", String.class);
        if (email == null || email.isBlank()) {
            return false;
        }
        String roleStr = claims.get("role", String.class);
        if (roleStr == null || roleStr.isBlank()) {
            return false;
        }
        try {
            UserRole.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }
}