package com.interntrack.api.security;

import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.AccountInactiveException;
import com.interntrack.api.exception.InvalidCredentialsException;
import com.interntrack.api.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain-level authentication service coordinating registration, credential verification,
 * and user lifecycle authentication.
 */
@Service
public class AuthenticationService {

    private final UserService userService;
    private final PasswordService passwordService;

    public AuthenticationService(UserService userService, PasswordService passwordService) {
        this.userService = userService;
        this.passwordService = passwordService;
    }

    /**
     * Registers a new candidate user account with default ROLE_USER.
     */
    @Transactional
    public AuthenticatedUser register(String email, String rawPassword, String fullName) {
        return register(email, rawPassword, fullName, UserRole.ROLE_USER);
    }

    /**
     * Registers a new user account.
     * Normalizes email, hashes the raw password, and persists through UserService.
     *
     * @param email       raw candidate email
     * @param rawPassword plaintext candidate password
     * @param fullName    candidate full name
     * @param role        user role
     * @return AuthenticatedUser principal representation (excluding password hash)
     */
    @Transactional
    public AuthenticatedUser register(String email, String rawPassword, String fullName, UserRole role) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be null or blank");
        }
        if (!passwordService.isValidPassword(rawPassword)) {
            throw new IllegalArgumentException("Password does not meet platform security requirements (must be 8-100 characters)");
        }

        String normalizedEmail = userService.normalizeEmail(email);
        String passwordHash = passwordService.hash(rawPassword);

        User createdUser = userService.createUser(normalizedEmail, passwordHash, fullName.trim(), role);

        return new AuthenticatedUser(
                createdUser.getId(),
                createdUser.getEmail(),
                createdUser.getFullName(),
                createdUser.getRole(),
                Boolean.TRUE.equals(createdUser.getIsActive())
        );
    }

    /**
     * Authenticates an existing user using email and plaintext password.
     *
     * @param email       candidate email
     * @param rawPassword candidate password
     * @return AuthenticatedUser principal representation (excluding password hash)
     * @throws InvalidCredentialsException if email not found or password does not match
     * @throws AccountInactiveException   if the user account has been deactivated
     */
    @Transactional(readOnly = true)
    public AuthenticatedUser authenticate(String email, String rawPassword) {
        if (email == null || email.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String normalizedEmail = userService.normalizeEmail(email);
        User user = userService.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AccountInactiveException("Account is inactive");
        }

        if (!passwordService.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                Boolean.TRUE.equals(user.getIsActive())
        );
    }
}