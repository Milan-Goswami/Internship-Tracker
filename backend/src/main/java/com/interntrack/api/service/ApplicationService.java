package com.interntrack.api.service;

import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.Opportunity;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.ApplicationStatus;
import com.interntrack.api.exception.DuplicateApplicationException;
import com.interntrack.api.exception.InvalidStatusTransitionException;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.ApplicationRepository;
import com.interntrack.api.repository.OpportunityRepository;
import com.interntrack.api.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final OpportunityRepository opportunityRepository;

    private static final Set<ApplicationStatus> TERMINAL_STATUSES = Set.of(
            ApplicationStatus.ACCEPTED,
            ApplicationStatus.REJECTED,
            ApplicationStatus.WITHDRAWN
    );

    public ApplicationService(ApplicationRepository applicationRepository,
                              UserRepository userRepository,
                              OpportunityRepository opportunityRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.opportunityRepository = opportunityRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Application> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return applicationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Application getApplicationById(Long applicationId, Long userId) {
        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Application application = applicationRepository.findByIdWithDetails(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        if (application.getUser() == null || !Objects.equals(application.getUser().getId(), userId)) {
            throw new AccessDeniedException("Unauthorized access to requested application");
        }

        return application;
    }

    @Transactional(readOnly = true)
    public List<Application> findByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return applicationRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Application> getApplicationsForUser(Long userId, ApplicationStatus status) {
        if (userId == null) {
            return List.of();
        }
        return applicationRepository.findByUserIdAndOptionalStatusWithDetails(userId, status);
    }

    @Transactional(readOnly = true)
    public Optional<Application> findByUserIdAndOpportunityId(Long userId, Long opportunityId) {
        if (userId == null || opportunityId == null) {
            return Optional.empty();
        }
        return applicationRepository.findByUserIdAndOpportunityId(userId, opportunityId);
    }

    @Transactional(readOnly = true)
    public boolean existsByUserIdAndOpportunityId(Long userId, Long opportunityId) {
        if (userId == null || opportunityId == null) {
            return false;
        }
        return applicationRepository.existsByUserIdAndOpportunityId(userId, opportunityId);
    }

    @Transactional(readOnly = true)
    public List<Application> findByUserIdAndStatus(Long userId, ApplicationStatus status) {
        if (userId == null || status == null) {
            return List.of();
        }
        return applicationRepository.findByUserIdAndStatus(userId, status);
    }

    @Transactional
    public Application createApplication(
            Long userId,
            Long opportunityId,
            ApplicationStatus initialStatus,
            Long resumeDocumentId,
            Long coverLetterDocumentId,
            String personalNotes,
            Integer userRating) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (opportunityId == null) {
            throw new IllegalArgumentException("Opportunity ID cannot be null");
        }
        if (userRating != null && (userRating < 1 || userRating > 5)) {
            throw new IllegalArgumentException("User rating must be between 1 and 5");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Opportunity opportunity = opportunityRepository.findByIdWithCompany(opportunityId).orElseGet(() -> opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + opportunityId)));

        if (applicationRepository.existsByUserIdAndOpportunityId(userId, opportunityId)) {
            throw new DuplicateApplicationException(
                    "Application already exists for user ID " + userId + " and opportunity ID " + opportunityId
            );
        }

        ApplicationStatus status = initialStatus != null ? initialStatus : ApplicationStatus.SAVED;
        Instant now = Instant.now();

        Application application = new Application();
        application.setUser(user);
        application.setOpportunity(opportunity);
        application.setStatus(status);
        application.setResumeDocumentId(resumeDocumentId);
        application.setCoverLetterDocumentId(coverLetterDocumentId);
        application.setPersonalNotes(personalNotes != null && !personalNotes.isBlank() ? personalNotes.trim() : null);
        application.setUserRating(userRating);

        application.setSavedAt(now);

        if (status == ApplicationStatus.APPLIED) {
            application.setAppliedAt(now);
        } else if (status == ApplicationStatus.SCREENING) {
            application.setAppliedAt(now);
            application.setScreeningAt(now);
        } else if (status == ApplicationStatus.INTERVIEWING) {
            application.setAppliedAt(now);
            application.setScreeningAt(now);
            application.setInterviewingAt(now);
        } else if (TERMINAL_STATUSES.contains(status)) {
            application.setClosedAt(now);
        }

        return applicationRepository.save(application);
    }

    @Transactional
    public Application transitionStatus(Long applicationId, Long userId, ApplicationStatus newStatus) {
        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("Target status cannot be null");
        }

        Application application = applicationRepository.findByIdWithDetails(applicationId).orElseGet(() -> applicationRepository.findById(applicationId).orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId)));

        if (application.getUser() == null || !Objects.equals(application.getUser().getId(), userId)) {
            throw new AccessDeniedException("Unauthorized: User does not own this application");
        }

        ApplicationStatus currentStatus = application.getStatus();

        if (currentStatus == newStatus) {
            return application;
        }

        validateTransition(currentStatus, newStatus);

        Instant now = Instant.now();

        switch (newStatus) {
            case APPLIED -> {
                if (application.getAppliedAt() == null) {
                    application.setAppliedAt(now);
                }
            }
            case SCREENING -> {
                if (application.getAppliedAt() == null) {
                    application.setAppliedAt(now);
                }
                if (application.getScreeningAt() == null) {
                    application.setScreeningAt(now);
                }
            }
            case INTERVIEWING -> {
                if (application.getAppliedAt() == null) {
                    application.setAppliedAt(now);
                }
                if (application.getInterviewingAt() == null) {
                    application.setInterviewingAt(now);
                }
            }
            case OFFER -> {
                if (application.getAppliedAt() == null) {
                    application.setAppliedAt(now);
                }
            }
            default -> {
            }
        }

        if (TERMINAL_STATUSES.contains(newStatus)) {
            application.setClosedAt(now);
        } else if (TERMINAL_STATUSES.contains(currentStatus) && !TERMINAL_STATUSES.contains(newStatus)) {
            application.setClosedAt(null);
        }

        application.setStatus(newStatus);

        return applicationRepository.save(application);
    }

    @Transactional
    public List<Application> batchTransitionStatus(List<Long> applicationIds, Long userId, ApplicationStatus newStatus) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            throw new IllegalArgumentException("Application IDs list cannot be empty");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("Target status cannot be null");
        }

        // Phase 1: Pre-flight validation phase across the ENTIRE batch before ANY mutation
        List<Application> validatedApplications = new ArrayList<>(applicationIds.size());
        for (Long appId : applicationIds) {
            if (appId == null) {
                throw new IllegalArgumentException("Application ID cannot be null");
            }
            Application application = applicationRepository.findByIdWithDetails(appId).orElseGet(() -> applicationRepository.findById(appId).orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + appId)));

            if (application.getUser() == null || !Objects.equals(application.getUser().getId(), userId)) {
                throw new AccessDeniedException("Unauthorized: User does not own application with id: " + appId);
            }

            ApplicationStatus currentStatus = application.getStatus();
            if (currentStatus != newStatus) {
                validateTransition(currentStatus, newStatus);
            }

            validatedApplications.add(application);
        }

        // Phase 2: Mutation phase executed ONLY after 100% of batch items pass validation
        Instant now = Instant.now();
        List<Application> updatedApplications = new ArrayList<>(validatedApplications.size());

        for (Application application : validatedApplications) {
            ApplicationStatus currentStatus = application.getStatus();
            if (currentStatus == newStatus) {
                updatedApplications.add(application);
                continue;
            }

            switch (newStatus) {
                case APPLIED -> {
                    if (application.getAppliedAt() == null) {
                        application.setAppliedAt(now);
                    }
                }
                case SCREENING -> {
                    if (application.getAppliedAt() == null) {
                        application.setAppliedAt(now);
                    }
                    if (application.getScreeningAt() == null) {
                        application.setScreeningAt(now);
                    }
                }
                case INTERVIEWING -> {
                    if (application.getAppliedAt() == null) {
                        application.setAppliedAt(now);
                    }
                    if (application.getInterviewingAt() == null) {
                        application.setInterviewingAt(now);
                    }
                }
                case OFFER -> {
                    if (application.getAppliedAt() == null) {
                        application.setAppliedAt(now);
                    }
                }
                default -> {
                }
            }

            if (TERMINAL_STATUSES.contains(newStatus)) {
                application.setClosedAt(now);
            } else if (TERMINAL_STATUSES.contains(currentStatus) && !TERMINAL_STATUSES.contains(newStatus)) {
                application.setClosedAt(null);
            }

            application.setStatus(newStatus);
            Application saved = applicationRepository.save(application);
            initializeLazyProxies(saved);
            updatedApplications.add(saved);
        }

        return updatedApplications;
    }

    @Transactional
    public Application updateNotesAndRating(
            Long applicationId,
            Long userId,
            String personalNotes,
            Integer userRating,
            Long resumeDocumentId,
            Long coverLetterDocumentId) {

        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (userRating != null && (userRating < 1 || userRating > 5)) {
            throw new IllegalArgumentException("User rating must be between 1 and 5");
        }

        Application application = applicationRepository.findByIdWithDetails(applicationId).orElseGet(() -> applicationRepository.findById(applicationId).orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId)));

        if (application.getUser() == null || !Objects.equals(application.getUser().getId(), userId)) {
            throw new AccessDeniedException("Unauthorized: User does not own this application");
        }

        application.setPersonalNotes(personalNotes != null && !personalNotes.isBlank() ? personalNotes.trim() : null);
        application.setUserRating(userRating);
        if (resumeDocumentId != null) {
            application.setResumeDocumentId(resumeDocumentId);
        }
        if (coverLetterDocumentId != null) {
            application.setCoverLetterDocumentId(coverLetterDocumentId);
        }

        return applicationRepository.save(application);
    }

    @Transactional
    public void deleteApplication(Long applicationId, Long userId) {
        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Application application = applicationRepository.findByIdWithDetails(applicationId).orElseGet(() -> applicationRepository.findById(applicationId).orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId)));

        if (application.getUser() == null || !Objects.equals(application.getUser().getId(), userId)) {
            throw new AccessDeniedException("Unauthorized: User does not own this application");
        }

        applicationRepository.delete(application);
        applicationRepository.flush();
    }

    private void initializeLazyProxies(Application app) {
        if (app != null && app.getOpportunity() != null) {
            try {
                app.getOpportunity().getTitle();
                if (app.getOpportunity().getCompany() != null) {
                    app.getOpportunity().getCompany().getName();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void validateTransition(ApplicationStatus fromStatus, ApplicationStatus toStatus) {
        boolean valid = switch (fromStatus) {
            case SAVED -> toStatus == ApplicationStatus.APPLIED
                    || toStatus == ApplicationStatus.WITHDRAWN
                    || toStatus == ApplicationStatus.REJECTED;

            case APPLIED -> toStatus == ApplicationStatus.SCREENING
                    || toStatus == ApplicationStatus.INTERVIEWING
                    || toStatus == ApplicationStatus.OFFER
                    || toStatus == ApplicationStatus.REJECTED
                    || toStatus == ApplicationStatus.WITHDRAWN
                    || toStatus == ApplicationStatus.SAVED;

            case SCREENING -> toStatus == ApplicationStatus.INTERVIEWING
                    || toStatus == ApplicationStatus.OFFER
                    || toStatus == ApplicationStatus.REJECTED
                    || toStatus == ApplicationStatus.WITHDRAWN
                    || toStatus == ApplicationStatus.APPLIED;

            case INTERVIEWING -> toStatus == ApplicationStatus.OFFER
                    || toStatus == ApplicationStatus.REJECTED
                    || toStatus == ApplicationStatus.WITHDRAWN
                    || toStatus == ApplicationStatus.SCREENING;

            case OFFER -> toStatus == ApplicationStatus.ACCEPTED
                    || toStatus == ApplicationStatus.REJECTED
                    || toStatus == ApplicationStatus.WITHDRAWN
                    || toStatus == ApplicationStatus.INTERVIEWING;

            case ACCEPTED -> toStatus == ApplicationStatus.REJECTED
                    || toStatus == ApplicationStatus.WITHDRAWN
                    || toStatus == ApplicationStatus.OFFER;

            case REJECTED -> toStatus == ApplicationStatus.SAVED
                    || toStatus == ApplicationStatus.APPLIED
                    || toStatus == ApplicationStatus.SCREENING
                    || toStatus == ApplicationStatus.INTERVIEWING
                    || toStatus == ApplicationStatus.OFFER;

            case WITHDRAWN -> toStatus == ApplicationStatus.SAVED
                    || toStatus == ApplicationStatus.APPLIED
                    || toStatus == ApplicationStatus.SCREENING
                    || toStatus == ApplicationStatus.INTERVIEWING
                    || toStatus == ApplicationStatus.OFFER;
        };

        if (!valid) {
            throw new InvalidStatusTransitionException(
                    "Invalid status transition from " + fromStatus + " to " + toStatus
            );
        }
    }
}