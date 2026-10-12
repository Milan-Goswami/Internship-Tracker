package com.interntrack.api.dto.application;

import com.interntrack.api.entity.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationStatusRequest(
        @NotNull(message = "Status is required")
        ApplicationStatus status
) {
}
