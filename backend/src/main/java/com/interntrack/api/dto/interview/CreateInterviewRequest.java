package com.interntrack.api.dto.interview;

import com.interntrack.api.entity.enums.InterviewRoundType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateInterviewRequest(
        @NotNull(message = "Round number is required")
        @Positive(message = "Round number must be a positive integer")
        Integer roundNumber,

        @NotBlank(message = "Round title is required")
        @Size(max = 150, message = "Round title cannot exceed 150 characters")
        String roundTitle,

        @NotNull(message = "Round type is required")
        InterviewRoundType roundType,

        @NotNull(message = "ScheduledAt timestamp is required")
        Instant scheduledAt,

        @Positive(message = "Duration minutes must be positive")
        Integer durationMinutes,

        @Size(max = 255, message = "Interviewer names cannot exceed 255 characters")
        String interviewerNames,

        @Size(max = 255, message = "Interviewer emails cannot exceed 255 characters")
        String interviewerEmails,

        String prepNotes
) {
}
