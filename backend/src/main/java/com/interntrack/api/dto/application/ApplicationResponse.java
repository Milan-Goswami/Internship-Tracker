package com.interntrack.api.dto.application;

import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.enums.ApplicationStatus;

import java.time.Instant;

public record ApplicationResponse(
        Long id,
        Long opportunityId,
        String opportunityTitle,
        Long companyId,
        String companyName,
        String companyLogoUrl,
        ApplicationStatus status,
        Long resumeDocumentId,
        Long coverLetterDocumentId,
        String personalNotes,
        Integer userRating,
        Instant savedAt,
        Instant appliedAt,
        Instant screeningAt,
        Instant interviewingAt,
        Instant closedAt,
        Instant updatedAt
) {
    public static ApplicationResponse from(Application application) {
        if (application == null) {
            return null;
        }
        return new ApplicationResponse(
                application.getId(),
                application.getOpportunity() != null ? application.getOpportunity().getId() : null,
                application.getOpportunity() != null ? application.getOpportunity().getTitle() : null,
                application.getOpportunity() != null && application.getOpportunity().getCompany() != null
                        ? application.getOpportunity().getCompany().getId() : null,
                application.getOpportunity() != null && application.getOpportunity().getCompany() != null
                        ? application.getOpportunity().getCompany().getName() : null,
                application.getOpportunity() != null && application.getOpportunity().getCompany() != null
                        ? application.getOpportunity().getCompany().getLogoUrl() : null,
                application.getStatus(),
                application.getResumeDocumentId(),
                application.getCoverLetterDocumentId(),
                application.getPersonalNotes(),
                application.getUserRating(),
                application.getSavedAt(),
                application.getAppliedAt(),
                application.getScreeningAt(),
                application.getInterviewingAt(),
                application.getClosedAt(),
                application.getUpdatedAt()
        );
    }
}
