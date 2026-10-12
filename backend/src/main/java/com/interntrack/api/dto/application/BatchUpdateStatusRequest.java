package com.interntrack.api.dto.application;

import com.interntrack.api.entity.enums.ApplicationStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BatchUpdateStatusRequest(
        @NotEmpty(message = "Application IDs list cannot be empty")
        List<@NotNull(message = "Application ID cannot be null") Long> applicationIds,

        @NotNull(message = "Status is required")
        ApplicationStatus status
) {
}
