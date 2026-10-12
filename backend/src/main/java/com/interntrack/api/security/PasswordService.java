package com.interntrack.api.security;

/**
 * Clean abstraction for secure password hashing and verification.
 * Decouples authentication logic from specific hashing algorithms and implementations.
 */
public interface PasswordService {

    /**
     * Hashes a raw password securely using an industry-standard adaptive hashing mechanism.
     *
     * @param rawPassword the raw plaintext password
     * @return the encoded password hash string
     * @throws IllegalArgumentException if rawPassword is null, blank, or violates policy constraints
     */
    String hash(String rawPassword);

    /**
     * Verifies whether a raw password matches an existing encoded password hash.
     *
     * @param rawPassword the raw plaintext password
     * @param encodedHash the stored encoded hash
     * @return true if the password matches the hash, false otherwise
     */
    boolean matches(String rawPassword, String encodedHash);

    /**
     * Evaluates whether a raw password meets the platform security policy (non-null, non-blank, length bounded).
     *
     * @param rawPassword the raw password to evaluate
     * @return true if valid according to policy, false otherwise
     */
    boolean isValidPassword(String rawPassword);
}