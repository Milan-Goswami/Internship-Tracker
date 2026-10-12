package com.interntrack.api.service;

import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.Company;
import com.interntrack.api.entity.Opportunity;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.ApplicationStatus;
import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.SourceProvider;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.DuplicateApplicationException;
import com.interntrack.api.exception.InvalidStatusTransitionException;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.ApplicationRepository;
import com.interntrack.api.repository.OpportunityRepository;
import com.interntrack.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OpportunityRepository opportunityRepository;

    @InjectMocks
    private ApplicationService applicationService;

    private User sampleUser;
    private Opportunity sampleOpportunity;
    private Application sampleApplication;

    @BeforeEach
    void setUp() {
        sampleUser = new User("candidate@example.com", "hash123", "Candidate", UserRole.ROLE_USER);
        Company company = new Company("Google", "google.com", "Tech");
        sampleOpportunity = new Opportunity(company, "SWE Intern", LocationType.REMOTE, SourceProvider.SYSTEM);
        sampleApplication = new Application(sampleUser, sampleOpportunity, ApplicationStatus.SAVED);
    }

    @Test
    @DisplayName("findById returns application when found")
    void findById_whenFound_returnsApplication() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(sampleApplication));

        Optional<Application> result = applicationService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(ApplicationStatus.SAVED);
    }

    @Test
    @DisplayName("findById returns empty when not found or id is null")
    void findById_whenNotFoundOrNull_returnsEmpty() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(applicationService.findById(99L)).isEmpty();
        assertThat(applicationService.findById(null)).isEmpty();
    }

    @Test
    @DisplayName("getApplicationById returns application when user is owner")
    void getApplicationById_whenOwner_returnsApplication() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleApplication.setUser(owner);

        when(applicationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(sampleApplication));

        Application result = applicationService.getApplicationById(1L, 10L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.SAVED);
    }

    @Test
    @DisplayName("getApplicationById throws AccessDeniedException when requested by non-owner")
    void getApplicationById_whenNotOwner_throwsAccessDeniedException() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleApplication.setUser(owner);

        when(applicationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(sampleApplication));

        assertThatThrownBy(() -> applicationService.getApplicationById(1L, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthorized access to requested application");
    }

    @Test
    @DisplayName("getApplicationById throws ResourceNotFoundException when application does not exist")
    void getApplicationById_whenNotFound_throwsResourceNotFoundException() {
        when(applicationRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationById(999L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found with id: 999");
    }

    @Test
    @DisplayName("getApplicationById throws IllegalArgumentException on null arguments")
    void getApplicationById_whenNullArgs_throwsException() {
        assertThatThrownBy(() -> applicationService.getApplicationById(null, 10L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> applicationService.getApplicationById(1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("findByUserId returns applications list")
    void findByUserId_returnsApplications() {
        when(applicationRepository.findByUserId(10L)).thenReturn(List.of(sampleApplication));

        List<Application> results = applicationService.findByUserId(10L);

        assertThat(results).hasSize(1);
        assertThat(applicationService.findByUserId(null)).isEmpty();
    }

    @Test
    @DisplayName("getApplicationsForUser returns applications with details")
    void getApplicationsForUser_returnsApplications() {
        when(applicationRepository.findByUserIdAndOptionalStatusWithDetails(10L, ApplicationStatus.SAVED))
                .thenReturn(List.of(sampleApplication));

        List<Application> results = applicationService.getApplicationsForUser(10L, ApplicationStatus.SAVED);

        assertThat(results).hasSize(1);
        assertThat(applicationService.getApplicationsForUser(null, null)).isEmpty();
    }

    @Test
    @DisplayName("createApplication persists new application with initial SAVED status and sets savedAt")
    void createApplication_withInitialSaved_populatesSavedAt() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(opportunityRepository.findById(2L)).thenReturn(Optional.of(sampleOpportunity));
        when(applicationRepository.existsByUserIdAndOpportunityId(1L, 2L)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Application created = applicationService.createApplication(
                1L, 2L, ApplicationStatus.SAVED, null, null, "My notes", 4
        );

        assertThat(created.getUser()).isEqualTo(sampleUser);
        assertThat(created.getOpportunity()).isEqualTo(sampleOpportunity);
        assertThat(created.getStatus()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(created.getSavedAt()).isNotNull();
        assertThat(created.getAppliedAt()).isNull();
        assertThat(created.getUserRating()).isEqualTo(4);
        assertThat(created.getPersonalNotes()).isEqualTo("My notes");

        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ApplicationStatus.SAVED);
    }

    @Test
    @DisplayName("createApplication throws ResourceNotFoundException when user missing")
    void createApplication_whenUserMissing_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.createApplication(
                99L, 2L, ApplicationStatus.SAVED, null, null, null, null
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createApplication throws ResourceNotFoundException when opportunity missing")
    void createApplication_whenOpportunityMissing_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(opportunityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.createApplication(
                1L, 99L, ApplicationStatus.SAVED, null, null, null, null
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Opportunity not found");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createApplication throws DuplicateApplicationException when already tracked")
    void createApplication_whenDuplicate_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(opportunityRepository.findById(2L)).thenReturn(Optional.of(sampleOpportunity));
        when(applicationRepository.existsByUserIdAndOpportunityId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.createApplication(
                1L, 2L, ApplicationStatus.SAVED, null, null, null, null
        ))
                .isInstanceOf(DuplicateApplicationException.class)
                .hasMessageContaining("Application already exists");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("transitionStatus executes valid lifecycle transition and populates first-entry timestamp")
    void transitionStatus_validTransition_updatesStatusAndTimestamps() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        sampleApplication.setUser(user);
        sampleApplication.setStatus(ApplicationStatus.SAVED);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(sampleApplication));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Application updated = applicationService.transitionStatus(1L, 10L, ApplicationStatus.APPLIED);

        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(updated.getAppliedAt()).isNotNull();
        assertThat(updated.getClosedAt()).isNull();
    }

    @Test
    @DisplayName("transitionStatus preserves first-entry timestamp and does not overwrite on subsequent entries")
    void transitionStatus_preservesOriginalFirstEntryTimestamp() {
        Instant originalAppliedAt = Instant.parse("2026-01-15T10:00:00Z");
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);

        sampleApplication.setUser(user);
        sampleApplication.setStatus(ApplicationStatus.APPLIED);
        sampleApplication.setAppliedAt(originalAppliedAt);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(sampleApplication));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Application updated = applicationService.transitionStatus(1L, 10L, ApplicationStatus.SCREENING);

        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.SCREENING);
        assertThat(updated.getAppliedAt()).isEqualTo(originalAppliedAt);
        assertThat(updated.getScreeningAt()).isNotNull();
    }

    @Test
    @DisplayName("transitionStatus throws InvalidStatusTransitionException on illegal jump")
    void transitionStatus_invalidTransition_throwsException() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        sampleApplication.setUser(user);
        sampleApplication.setStatus(ApplicationStatus.SAVED);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(sampleApplication));

        assertThatThrownBy(() -> applicationService.transitionStatus(1L, 10L, ApplicationStatus.OFFER))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("SAVED")
                .hasMessageContaining("OFFER");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("transitionStatus throws AccessDeniedException when caller does not own the application")
    void transitionStatus_unauthorizedUser_throwsAccessDeniedException() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleApplication.setUser(owner);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(sampleApplication));

        assertThatThrownBy(() -> applicationService.transitionStatus(1L, 99L, ApplicationStatus.APPLIED))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthorized");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("transitionStatus populates closedAt for terminal outcomes and clears it on reopening")
    void transitionStatus_terminalOutcomeAndReopening() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        sampleApplication.setUser(user);
        sampleApplication.setStatus(ApplicationStatus.APPLIED);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(sampleApplication));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Application rejected = applicationService.transitionStatus(1L, 10L, ApplicationStatus.REJECTED);
        assertThat(rejected.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(rejected.getClosedAt()).isNotNull();

        Application reopened = applicationService.transitionStatus(1L, 10L, ApplicationStatus.APPLIED);
        assertThat(reopened.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(reopened.getClosedAt()).isNull();
    }

    @Test
    @DisplayName("updateNotesAndRating updates personal notes and ratings for owner")
    void updateNotesAndRating_whenOwner_updatesFields() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        sampleApplication.setUser(user);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(sampleApplication));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Application updated = applicationService.updateNotesAndRating(
                1L, 10L, "New recruiter notes", 5, 55L, 66L
        );

        assertThat(updated.getPersonalNotes()).isEqualTo("New recruiter notes");
        assertThat(updated.getUserRating()).isEqualTo(5);
        assertThat(updated.getResumeDocumentId()).isEqualTo(55L);
        assertThat(updated.getCoverLetterDocumentId()).isEqualTo(66L);
    }

    @Test
    @DisplayName("updateNotesAndRating throws AccessDeniedException when caller is not owner")
    void updateNotesAndRating_whenNotOwner_throwsAccessDeniedException() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleApplication.setUser(owner);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(sampleApplication));

        assertThatThrownBy(() -> applicationService.updateNotesAndRating(
                1L, 99L, "Hacker notes", 5, null, null
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthorized");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteApplication deletes and flushes when caller is owner")
    void deleteApplication_whenOwner_deletesAndFlushes() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleApplication.setUser(owner);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(sampleApplication));

        applicationService.deleteApplication(1L, 10L);

        verify(applicationRepository).delete(sampleApplication);
        verify(applicationRepository).flush();
    }

    @Test
    @DisplayName("deleteApplication throws AccessDeniedException when caller is not owner")
    void deleteApplication_whenNotOwner_throwsAccessDeniedException() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleApplication.setUser(owner);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(sampleApplication));

        assertThatThrownBy(() -> applicationService.deleteApplication(1L, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthorized");

        verify(applicationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("batchTransitionStatus updates all applications atomically when all valid and owned")
    void batchTransitionStatus_whenAllOwnedAndValid_transitionsAllAtomically() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);

        Application app1 = new Application(owner, sampleOpportunity, ApplicationStatus.SAVED);
        Application app2 = new Application(owner, sampleOpportunity, ApplicationStatus.SAVED);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app1));
        when(applicationRepository.findById(2L)).thenReturn(Optional.of(app2));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Application> results = applicationService.batchTransitionStatus(List.of(1L, 2L), 10L, ApplicationStatus.APPLIED);

        assertThat(results).hasSize(2);
        assertThat(app1.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(app2.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(app1.getAppliedAt()).isNotNull();
        assertThat(app2.getAppliedAt()).isNotNull();
        verify(applicationRepository, times(2)).save(any(Application.class));
    }

    @Test
    @DisplayName("batchTransitionStatus throws AccessDeniedException and modifies ZERO applications on mixed-ownership batch")
    void batchTransitionStatus_whenMixedBatchWithUnownedApplication_throwsAccessDeniedExceptionAndModifiesZero() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);

        User otherUser = mock(User.class);
        when(otherUser.getId()).thenReturn(20L);

        Application app1 = new Application(owner, sampleOpportunity, ApplicationStatus.SAVED);
        Application app2 = new Application(otherUser, sampleOpportunity, ApplicationStatus.SAVED);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app1));
        when(applicationRepository.findById(2L)).thenReturn(Optional.of(app2));

        // User 10 attempts batch update with [1L, 2L] where 2L belongs to User 20
        assertThatThrownBy(() -> applicationService.batchTransitionStatus(List.of(1L, 2L), 10L, ApplicationStatus.APPLIED))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthorized: User does not own application with id: 2");

        // Zero applications saved
        verify(applicationRepository, never()).save(any());
        // Zero applications modified in memory
        assertThat(app1.getStatus()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(app2.getStatus()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(app1.getAppliedAt()).isNull();
    }

    @Test
    @DisplayName("batchTransitionStatus throws InvalidStatusTransitionException and modifies ZERO applications on invalid jump")
    void batchTransitionStatus_whenInvalidTransitionInBatch_throwsExceptionAndModifiesZero() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);

        Application app1 = new Application(owner, sampleOpportunity, ApplicationStatus.APPLIED);
        Application app2 = new Application(owner, sampleOpportunity, ApplicationStatus.SAVED);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app1));
        when(applicationRepository.findById(2L)).thenReturn(Optional.of(app2));

        // APPLIED -> SCREENING is valid for app1, but SAVED -> SCREENING is invalid for app2
        assertThatThrownBy(() -> applicationService.batchTransitionStatus(List.of(1L, 2L), 10L, ApplicationStatus.SCREENING))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Invalid status transition");

        verify(applicationRepository, never()).save(any());
        assertThat(app1.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(app2.getStatus()).isEqualTo(ApplicationStatus.SAVED);
    }
}