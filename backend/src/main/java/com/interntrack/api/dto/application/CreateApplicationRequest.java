package com.interntrack.api.dto.application;

import com.interntrack.api.entity.enums.ApplicationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(
        @NotNull(message = "Opportunity ID is required")
        Long opportunityId,

        ApplicationStatus status,

        Long resumeDocumentId,

        Long coverLetterDocumentId,

        @Size(max = 10000, message = "Personal notes cannot exceed 10000 characters")
        String personalNotes,

        @Min(value = 1, message = "User rating must be at least 1")
        @Max(value = 5, message = "User rating cannot exceed 5")
        Integer userRating
) {
}
