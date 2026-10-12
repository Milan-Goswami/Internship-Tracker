package com.interntrack.api.dto.auth;

import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.UserRole;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        UserRole role,
        Boolean isActive,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt()
        );
    }
}