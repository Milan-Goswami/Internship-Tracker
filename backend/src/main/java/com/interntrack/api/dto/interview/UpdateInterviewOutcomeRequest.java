package com.interntrack.api.dto.interview;

import com.interntrack.api.entity.enums.InterviewOutcome;
import jakarta.validation.constraints.NotNull;

public record UpdateInterviewOutcomeRequest(
        @NotNull(message = "Outcome is required")
        InterviewOutcome outcome,

        String reflectionNotes
) {
}
