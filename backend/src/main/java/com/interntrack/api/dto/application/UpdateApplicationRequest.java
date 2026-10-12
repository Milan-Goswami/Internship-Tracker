package com.interntrack.api.dto.application;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateApplicationRequest(
        @Size(max = 10000, message = "Personal notes cannot exceed 10000 characters")
        String personalNotes,

        @Min(value = 1, message = "User rating must be at least 1")
        @Max(value = 5, message = "User rating cannot exceed 5")
        Integer userRating,

        Long resumeDocumentId,

        Long coverLetterDocumentId
) {
}
