package com.interntrack.api.dto.interview;

import com.interntrack.api.entity.Interview;
import com.interntrack.api.entity.enums.InterviewOutcome;
import com.interntrack.api.entity.enums.InterviewRoundType;

import java.time.Instant;

public record InterviewResponse(
        Long id,
        Long applicationId,
        Integer roundNumber,
        String roundTitle,
        InterviewRoundType roundType,
        Instant scheduledAt,
        Integer durationMinutes,
        String interviewerNames,
        String interviewerEmails,
        String prepNotes,
        String reflectionNotes,
        InterviewOutcome outcome,
        Instant createdAt,
        Instant updatedAt
) {
    public static InterviewResponse from(Interview interview) {
        if (interview == null) {
            return null;
        }
        return new InterviewResponse(
                interview.getId(),
                interview.getApplication() != null ? interview.getApplication().getId() : null,
                interview.getRoundNumber(),
                interview.getRoundTitle(),
                interview.getRoundType(),
                interview.getScheduledAt(),
                interview.getDurationMinutes(),
                interview.getInterviewerNames(),
                interview.getInterviewerEmails(),
                interview.getPrepNotes(),
                interview.getReflectionNotes(),
                interview.getOutcome(),
                interview.getCreatedAt(),
                interview.getUpdatedAt()
        );
    }
}
