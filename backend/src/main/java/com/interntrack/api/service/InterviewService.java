package com.interntrack.api.service;

import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.Interview;
import com.interntrack.api.entity.enums.InterviewOutcome;
import com.interntrack.api.entity.enums.InterviewRoundType;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.ApplicationRepository;
import com.interntrack.api.repository.InterviewRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;

    public InterviewService(InterviewRepository interviewRepository,
                            ApplicationRepository applicationRepository) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
    }

    /**
     * Looks up an interview by its primary key ID.
     */
    @Transactional(readOnly = true)
    public Optional<Interview> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return interviewRepository.findById(id);
    }

    /**
     * Retrieves all interview rounds for an application ordered chronologically.
     */
    @Transactional(readOnly = true)
    public List<Interview> findByApplicationId(Long applicationId) {
        if (applicationId == null) {
            return List.of();
        }
        return interviewRepository.findByApplicationIdOrderByScheduledAtAsc(applicationId);
    }

    /**
     * Retrieves all interview rounds for a specific application after verifying ownership.
     */
    @Transactional(readOnly = true)
    public List<Interview> getInterviewsForApplication(Long applicationId, Long userId) {
        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        if (application.getUser() == null || !Objects.equals(application.getUser().getId(), userId)) {
            throw new AccessDeniedException("Unauthorized: User does not own the associated application");
        }

        return interviewRepository.findByApplicationIdOrderByScheduledAtAsc(applicationId);
    }

    /**
     * Retrieves a single interview round by ID after verifying application ownership.
     */
    @Transactional(readOnly = true)
    public Interview getInterviewById(Long interviewId, Long userId) {
        if (interviewId == null) {
            throw new IllegalArgumentException("Interview ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Interview interview = interviewRepository.findByIdWithDetails(interviewId)
                .or(() -> interviewRepository.findById(interviewId))
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + interviewId));

        Application application = interview.getApplication();
        if (application == null || application.getUser() == null || !Objects.equals(application.getUser().getId(), userId)) {
            throw new AccessDeniedException("Unauthorized: User does not own the associated application");
        }

        return interview;
    }

    /**
     * Creates and schedules a new interview round attached to an application.
     */
    @Transactional
    public Interview createInterview(
            Long applicationId,
            Long userId,
            Integer roundNumber,
            String roundTitle,
            InterviewRoundType roundType,
            Instant scheduledAt,
            Integer durationMinutes,
            String interviewerNames,
            String interviewerEmails,
            String prepNotes) {

        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (roundNumber == null || roundNumber <= 0) {
            throw new IllegalArgumentException("Round number must be a positive integer");
        }
        if (roundTitle == null || roundTitle.isBlank()) {
            throw new IllegalArgumentException("Round title cannot be null or blank");
        }
        if (roundType == null) {
            throw new IllegalArgumentException("Round type cannot be null");
        }
        if (scheduledAt == null) {
            throw new IllegalArgumentException("ScheduledAt timestamp cannot be null");
        }
        if (durationMinutes != null && durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration minutes must be greater than zero");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        // Enforce application domain ownership
        if (application.getUser() == null || !Objects.equals(application.getUser().getId(), userId)) {
            throw new AccessDeniedException("Unauthorized: User does not own the associated application");
        }

        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setRoundNumber(roundNumber);
        interview.setRoundTitle(roundTitle.trim());
        interview.setRoundType(roundType);
        interview.setScheduledAt(scheduledAt);
        interview.setDurationMinutes(durationMinutes != null ? durationMinutes : 60);
        interview.setInterviewerNames(interviewerNames != null && !interviewerNames.isBlank() ? interviewerNames.trim() : null);
        interview.setInterviewerEmails(interviewerEmails != null && !interviewerEmails.isBlank() ? interviewerEmails.trim() : null);
        interview.setPrepNotes(prepNotes != null && !prepNotes.isBlank() ? prepNotes.trim() : null);
        interview.setOutcome(InterviewOutcome.PENDING);

        return interviewRepository.save(interview);
    }

    /**
     * Updates mutable fields of an existing interview round (11-parameter overload for backward compatibility).
     */
    @Transactional
    public Interview updateInterview(
            Long interviewId,
            Long userId,
            Integer roundNumber,
            String roundTitle,
            InterviewRoundType roundType,
            Instant scheduledAt,
            Integer durationMinutes,
            String interviewerNames,
            String interviewerEmails,
            String prepNotes,
            String reflectionNotes) {
        return updateInterview(interviewId, userId, roundNumber, roundTitle, roundType,
                scheduledAt, durationMinutes, interviewerNames, interviewerEmails, prepNotes, reflectionNotes, null);
    }

    /**
     * Updates mutable fields of an existing interview round including optional outcome.
     */
    @Transactional
    public Interview updateInterview(
            Long interviewId,
            Long userId,
            Integer roundNumber,
            String roundTitle,
            InterviewRoundType roundType,
            Instant scheduledAt,
            Integer durationMinutes,
            String interviewerNames,
            String interviewerEmails,
            String prepNotes,
            String reflectionNotes,
            InterviewOutcome outcome) {

        if (interviewId == null) {
            throw new IllegalArgumentException("Interview ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (roundNumber != null && roundNumber <= 0) {
            throw new IllegalArgumentException("Round number must be a positive integer");
        }
        if (durationMinutes != null && durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration minutes must be greater than zero");
        }

        Interview interview = interviewRepository.findByIdWithDetails(interviewId)
                .or(() -> interviewRepository.findById(interviewId))
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + interviewId));

        Application application = interview.getApplication();
        if (application == null || application.getUser() == null || !Objects.equals(application.getUser().getId(), userId)) {
            throw new AccessDeniedException("Unauthorized: User does not own the associated application");
        }

        if (roundNumber != null) {
            interview.setRoundNumber(roundNumber);
        }
        if (roundTitle != null && !roundTitle.isBlank()) {
            interview.setRoundTitle(roundTitle.trim());
        }
        if (roundType != null) {
            interview.setRoundType(roundType);
        }
        if (scheduledAt != null) {
            interview.setScheduledAt(scheduledAt);
        }
        if (durationMinutes != null) {
            interview.setDurationMinutes(durationMinutes);
        }
        if (interviewerNames != null) {
            interview.setInterviewerNames(!interviewerNames.isBlank() ? interviewerNames.trim() : null);
        }
        if (interviewerEmails != null) {
            interview.setInterviewerEmails(!interviewerEmails.isBlank() ? interviewerEmails.trim() : null);
        }
        if (prepNotes != null) {
            interview.setPrepNotes(!prepNotes.isBlank() ? prepNotes.trim() : null);
        }
        if (reflectionNotes != null) {
            interview.setReflectionNotes(!reflectionNotes.isBlank() ? reflectionNotes.trim() : null);
        }
        if (outcome != null) {
            interview.setOutcome(outcome);
        }

        return interviewRepository.save(interview);
    }

    /**
     * Records the outcome of an interview round.
     */
    @Transactional
    public Interview recordOutcome(Long interviewId, Long userId, InterviewOutcome outcome, String reflectionNotes) {
        if (interviewId == null) {
            throw new IllegalArgumentException("Interview ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (outcome == null) {
            throw new IllegalArgumentException("Interview outcome cannot be null");
        }

        Interview interview = interviewRepository.findByIdWithDetails(interviewId)
                .or(() -> interviewRepository.findById(interviewId))
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + interviewId));

        Application application = interview.getApplication();
        if (application == null || application.getUser() == null || !Objects.equals(application.getUser().getId(), userId)) {
            throw new AccessDeniedException("Unauthorized: User does not own the associated application");
        }

        interview.setOutcome(outcome);
        if (reflectionNotes != null) {
            interview.setReflectionNotes(!reflectionNotes.isBlank() ? reflectionNotes.trim() : null);
        }

        return interviewRepository.save(interview);
    }

    /**
     * Deletes an interview round after verifying ownership.
     */
    @Transactional
    public void deleteInterview(Long interviewId, Long userId) {
        if (interviewId == null) {
            throw new IllegalArgumentException("Interview ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Interview interview = interviewRepository.findByIdWithDetails(interviewId)
                .or(() -> interviewRepository.findById(interviewId))
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + interviewId));

        Application application = interview.getApplication();
        if (application == null || application.getUser() == null || !Objects.equals(application.getUser().getId(), userId)) {
            throw new AccessDeniedException("Unauthorized: User does not own the associated application");
        }

        interviewRepository.delete(interview);
    }
}
