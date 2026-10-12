package com.interntrack.api.security;

import com.interntrack.api.entity.enums.UserRole;

/**
 * Immutable value representation of an authenticated user principal.
 * Intentionally omits password hashes and internal credentials.
 */
public record AuthenticatedUser(
        Long id,
        String email,
        String fullName,
        UserRole role,
        boolean isActive
) {
}