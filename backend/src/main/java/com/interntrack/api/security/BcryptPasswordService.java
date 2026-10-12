package com.interntrack.api.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * BCrypt-backed implementation of PasswordService.
 * Enforces cost factor 12 and secure random salt generation per SECURITY_PLAN.md.
 */
@Service
public class BcryptPasswordService implements PasswordService {

    public static final int BCRYPT_STRENGTH = 12;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 100;

    private final PasswordEncoder passwordEncoder;

    public BcryptPasswordService() {
        this(new BCryptPasswordEncoder(BCRYPT_STRENGTH));
    }

    public BcryptPasswordService(PasswordEncoder passwordEncoder) {
        if (passwordEncoder == null) {
            throw new IllegalArgumentException("PasswordEncoder must not be null");
        }
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String rawPassword) {
        if (!isValidPassword(rawPassword)) {
            throw new IllegalArgumentException(
                    "Password must not be null or blank, and must be between " +
                    MIN_PASSWORD_LENGTH + " and " + MAX_PASSWORD_LENGTH + " characters."
            );
        }
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedHash) {
        if (rawPassword == null || rawPassword.isBlank() || encodedHash == null || encodedHash.isBlank()) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedHash);
    }

    @Override
    public boolean isValidPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return false;
        }
        int len = rawPassword.length();
        return len >= MIN_PASSWORD_LENGTH && len <= MAX_PASSWORD_LENGTH;
    }
}