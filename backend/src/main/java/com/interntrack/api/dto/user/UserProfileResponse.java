package com.interntrack.api.dto.user;

import com.interntrack.api.entity.User;
import com.interntrack.api.entity.UserProfile;
import com.interntrack.api.entity.enums.PreferredWorkMode;
import com.interntrack.api.entity.enums.UserRole;

import java.math.BigDecimal;
import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String email,
        String fullName,
        UserRole role,
        Boolean isActive,
        String targetTitle,
        String targetLocation,
        BigDecimal targetSalaryMin,
        BigDecimal targetSalaryMax,
        PreferredWorkMode preferredWorkMode,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserProfileResponse from(User user, UserProfile profile) {
        if (user == null) {
            return null;
        }
        if (profile == null) {
            return new UserProfileResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole(),
                    user.getIsActive(),
                    null,
                    null,
                    null,
                    null,
                    PreferredWorkMode.ANY,
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
        }
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getIsActive(),
                profile.getTargetTitle(),
                profile.getTargetLocation(),
                profile.getTargetSalaryMin(),
                profile.getTargetSalaryMax(),
                profile.getPreferredWorkMode(),
                user.getCreatedAt(),
                profile.getUpdatedAt() != null ? profile.getUpdatedAt() : user.getUpdatedAt()
        );
    }

    public static UserProfileResponse from(User user) {
        return from(user, null);
    }
}