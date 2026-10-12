package com.interntrack.api.dto.analytics;

import com.interntrack.api.entity.Interview;
import com.interntrack.api.entity.enums.InterviewRoundType;

import java.time.Instant;

public record UpcomingInterviewDto(
        Long id,
        Long applicationId,
        String companyName,
        String roleTitle,
        Integer roundNumber,
        String roundTitle,
        InterviewRoundType roundType,
        Instant scheduledAt,
        Integer durationMinutes
) {
    public static UpcomingInterviewDto from(Interview interview) {
        if (interview == null) {
            return null;
        }

        Long appId = null;
        String compName = null;
        String role = null;

        if (interview.getApplication() != null) {
            appId = interview.getApplication().getId();
            if (interview.getApplication().getOpportunity() != null) {
                role = interview.getApplication().getOpportunity().getTitle();
                if (interview.getApplication().getOpportunity().getCompany() != null) {
                    compName = interview.getApplication().getOpportunity().getCompany().getName();
                }
            }
        }

        return new UpcomingInterviewDto(
                interview.getId(),
                appId,
                compName,
                role,
                interview.getRoundNumber(),
                interview.getRoundTitle(),
                interview.getRoundType(),
                interview.getScheduledAt(),
                interview.getDurationMinutes()
        );
    }
}