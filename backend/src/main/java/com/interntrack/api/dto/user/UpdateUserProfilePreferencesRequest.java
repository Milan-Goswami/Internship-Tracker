package com.interntrack.api.dto.user;

import com.interntrack.api.entity.enums.PreferredWorkMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateUserProfilePreferencesRequest(
        @Size(max = 100, message = "Full name cannot exceed 100 characters")
        String fullName,

        @Size(max = 100, message = "Target title cannot exceed 100 characters")
        String targetTitle,

        @Size(max = 100, message = "Target location cannot exceed 100 characters")
        String targetLocation,

        @DecimalMin(value = "0.00", message = "Target minimum salary cannot be negative")
        BigDecimal targetSalaryMin,

        @DecimalMin(value = "0.00", message = "Target maximum salary cannot be negative")
        BigDecimal targetSalaryMax,

        PreferredWorkMode preferredWorkMode
) {
    public void validateSalaryRange() {
        if (targetSalaryMin != null && targetSalaryMax != null && targetSalaryMin.compareTo(targetSalaryMax) > 0) {
            throw new IllegalArgumentException("Target minimum salary cannot exceed target maximum salary");
        }
    }
}