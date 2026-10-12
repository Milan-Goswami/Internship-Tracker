package com.interntrack.api.service;

import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.Interview;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.InterviewOutcome;
import com.interntrack.api.entity.enums.InterviewRoundType;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.ApplicationRepository;
import com.interntrack.api.repository.InterviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private InterviewService interviewService;

    private Application sampleApplication;
    private Interview sampleInterview;

    @BeforeEach
    void setUp() {
        sampleApplication = new Application();
        ReflectionTestUtils.setField(sampleApplication, "id", 1L);

        sampleInterview = new Interview();
        ReflectionTestUtils.setField(sampleInterview, "id", 1L);
        sampleInterview.setApplication(sampleApplication);
        sampleInterview.setRoundNumber(1);
        sampleInterview.setRoundTitle("Initial Screen");
        sampleInterview.setRoundType(InterviewRoundType.RECRUITER_SCREEN);
        sampleInterview.setScheduledAt(Instant.now().plusSeconds(3600));
        sampleInterview.setDurationMinutes(30);
        sampleInterview.setOutcome(InterviewOutcome.PENDING);
    }

    @Test
    @DisplayName("findById returns empty when id is null")
    void findById_whenNullId_returnsEmpty() {
        assertThat(interviewService.findById(null)).isEmpty();
        verifyNoInteractions(interviewRepository);
    }

    @Test
    @DisplayName("findById returns interview when found")
    void findById_whenFound_returnsInterview() {
        when(interviewRepository.findById(1L)).thenReturn(Optional.of(sampleInterview));
        Optional<Interview> result = interviewService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getRoundTitle()).isEqualTo("Initial Screen");
    }

    @Test
    @DisplayName("findByApplicationId returns empty list when applicationId is null")
    void findByApplicationId_whenNullId_returnsEmptyList() {
        assertThat(interviewService.findByApplicationId(null)).isEmpty();
        verifyNoInteractions(interviewRepository);
    }

    @Test
    @DisplayName("findByApplicationId delegates to repository with ascending sort")
    void findByApplicationId_whenValid_returnsChronologicalList() {
        when(interviewRepository.findByApplicationIdOrderByScheduledAtAsc(1L))
                .thenReturn(List.of(sampleInterview));

        List<Interview> results = interviewService.findByApplicationId(1L);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRoundNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("getInterviewsForApplication returns rounds when user owns application")
    void getInterviewsForApplication_whenOwned_returnsRounds() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        sampleApplication.setUser(user);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(sampleApplication));
        when(interviewRepository.findByApplicationIdOrderByScheduledAtAsc(1L)).thenReturn(List.of(sampleInterview));

        List<Interview> results = interviewService.getInterviewsForApplication(1L, 10L);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRoundTitle()).isEqualTo("Initial Screen");
    }

    @Test
    @DisplayName("getInterviewsForApplication throws 404 when application missing")
    void getInterviewsForApplication_whenAppMissing_throws404() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.getInterviewsForApplication(99L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found");
    }

    @Test
    @DisplayName("getInterviewsForApplication throws 403 when user does not own application")
    void getInterviewsForApplication_whenOtherUser_throws403() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(20L);
        sampleApplication.setUser(owner);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(sampleApplication));

        assertThatThrownBy(() -> interviewService.getInterviewsForApplication(1L, 10L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    @DisplayName("getInterviewById returns interview when user owns parent application")
    void getInterviewById_whenOwned_returnsInterview() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleApplication.setUser(owner);
        sampleInterview.setApplication(sampleApplication);

        when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(sampleInterview));

        Interview result = interviewService.getInterviewById(1L, 10L);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getInterviewById throws 404 when interview not found")
    void getInterviewById_whenNotFound_throws404() {
        when(interviewRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());
        when(interviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.getInterviewById(99L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Interview not found");
    }

    @Test
    @DisplayName("getInterviewById throws 403 when user does not own parent application")
    void getInterviewById_whenOtherUser_throws403() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(20L);
        sampleApplication.setUser(owner);
        sampleInterview.setApplication(sampleApplication);

        when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(sampleInterview));

        assertThatThrownBy(() -> interviewService.getInterviewById(1L, 10L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    @DisplayName("createInterview successfully creates and persists interview")
    void createInterview_whenValid_createsAndSaves() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        sampleApplication.setUser(user);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(sampleApplication));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant schedule = Instant.now().plusSeconds(7200);
        Interview created = interviewService.createInterview(
                1L,
                10L,
                2,
                "  System Design Round  ",
                InterviewRoundType.SYSTEM_DESIGN,
                schedule,
                45,
                "Alex Engineer",
                "alex@stripe.com",
                "Review distributed systems concepts"
        );

        assertThat(created).isNotNull();
        assertThat(created.getRoundNumber()).isEqualTo(2);
        assertThat(created.getRoundTitle()).isEqualTo("System Design Round");
        assertThat(created.getRoundType()).isEqualTo(InterviewRoundType.SYSTEM_DESIGN);
        assertThat(created.getScheduledAt()).isEqualTo(schedule);
        assertThat(created.getDurationMinutes()).isEqualTo(45);
        assertThat(created.getInterviewerNames()).isEqualTo("Alex Engineer");
        assertThat(created.getInterviewerEmails()).isEqualTo("alex@stripe.com");
        assertThat(created.getPrepNotes()).isEqualTo("Review distributed systems concepts");
        assertThat(created.getOutcome()).isEqualTo(InterviewOutcome.PENDING);

        ArgumentCaptor<Interview> captor = ArgumentCaptor.forClass(Interview.class);
        verify(interviewRepository).save(captor.capture());
        assertThat(captor.getValue().getRoundNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("createInterview throws ResourceNotFoundException when application missing")
    void createInterview_whenApplicationMissing_throwsException() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.createInterview(
                99L, 10L, 1, "OA", InterviewRoundType.TECHNICAL_OA, Instant.now(), 60, null, null, null
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found");

        verify(interviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("createInterview throws IllegalArgumentException on invalid round number")
    void createInterview_whenInvalidRoundNumber_throwsException() {
        assertThatThrownBy(() -> interviewService.createInterview(
                1L, 10L, 0, "Title", InterviewRoundType.RECRUITER_SCREEN, Instant.now(), 60, null, null, null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Round number");

        assertThatThrownBy(() -> interviewService.createInterview(
                1L, 10L, -1, "Title", InterviewRoundType.RECRUITER_SCREEN, Instant.now(), 60, null, null, null
        ))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createInterview throws IllegalArgumentException on invalid duration")
    void createInterview_whenInvalidDuration_throwsException() {
        assertThatThrownBy(() -> interviewService.createInterview(
                1L, 10L, 1, "Title", InterviewRoundType.RECRUITER_SCREEN, Instant.now(), 0, null, null, null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration");
    }

    @Test
    @DisplayName("createInterview throws IllegalArgumentException on missing scheduledAt or blank title")
    void createInterview_whenMissingScheduleOrTitle_throwsException() {
        assertThatThrownBy(() -> interviewService.createInterview(
                1L, 10L, 1, "Title", InterviewRoundType.RECRUITER_SCREEN, null, 60, null, null, null
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> interviewService.createInterview(
                1L, 10L, 1, "   ", InterviewRoundType.RECRUITER_SCREEN, Instant.now(), 60, null, null, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createInterview throws AccessDeniedException when user is not owner of application")
    void createInterview_whenUnauthorized_throwsAccessDeniedException() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleApplication.setUser(owner);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(sampleApplication));

        assertThatThrownBy(() -> interviewService.createInterview(
                1L, 99L, 1, "OA", InterviewRoundType.TECHNICAL_OA, Instant.now(), 60, null, null, null
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthorized");

        verify(interviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateInterview updates mutable fields when requested by application owner")
    void updateInterview_whenValid_updatesFields() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleApplication.setUser(owner);
        sampleInterview.setApplication(sampleApplication);

        when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(sampleInterview));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant newSchedule = Instant.now().plusSeconds(10000);
        Interview updated = interviewService.updateInterview(
                1L,
                10L,
                2,
                "Updated Round Title",
                InterviewRoundType.EXECUTIVE_FINAL,
                newSchedule,
                90,
                "VP Eng",
                "vp@stripe.com",
                "New prep notes",
                "Good vibe",
                InterviewOutcome.PASSED
        );

        assertThat(updated.getRoundNumber()).isEqualTo(2);
        assertThat(updated.getRoundTitle()).isEqualTo("Updated Round Title");
        assertThat(updated.getRoundType()).isEqualTo(InterviewRoundType.EXECUTIVE_FINAL);
        assertThat(updated.getScheduledAt()).isEqualTo(newSchedule);
        assertThat(updated.getDurationMinutes()).isEqualTo(90);
        assertThat(updated.getInterviewerNames()).isEqualTo("VP Eng");
        assertThat(updated.getInterviewerEmails()).isEqualTo("vp@stripe.com");
        assertThat(updated.getPrepNotes()).isEqualTo("New prep notes");
        assertThat(updated.getReflectionNotes()).isEqualTo("Good vibe");
        assertThat(updated.getOutcome()).isEqualTo(InterviewOutcome.PASSED);
    }

    @Test
    @DisplayName("updateInterview throws 404 when interview missing")
    void updateInterview_whenMissing_throws404() {
        when(interviewRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());
        when(interviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.updateInterview(
                99L, 10L, 1, "Title", InterviewRoundType.BEHAVIORAL, Instant.now(), 60, null, null, null, null, null
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateInterview throws 403 when user is not owner")
    void updateInterview_whenOtherUser_throws403() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(20L);
        sampleApplication.setUser(owner);
        sampleInterview.setApplication(sampleApplication);

        when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(sampleInterview));

        assertThatThrownBy(() -> interviewService.updateInterview(
                1L, 10L, 1, "Title", InterviewRoundType.BEHAVIORAL, Instant.now(), 60, null, null, null, null, null
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("recordOutcome updates outcome and reflection notes")
    void recordOutcome_whenValid_updatesOutcome() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleApplication.setUser(owner);
        sampleInterview.setApplication(sampleApplication);

        when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(sampleInterview));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Interview updated = interviewService.recordOutcome(
                1L, 10L, InterviewOutcome.PASSED, "Crushed the coding exercise!"
        );

        assertThat(updated.getOutcome()).isEqualTo(InterviewOutcome.PASSED);
        assertThat(updated.getReflectionNotes()).isEqualTo("Crushed the coding exercise!");
    }

    @Test
    @DisplayName("recordOutcome throws IllegalArgumentException when outcome is null")
    void recordOutcome_whenNullOutcome_throwsException() {
        assertThatThrownBy(() -> interviewService.recordOutcome(1L, 10L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outcome");
    }

    @Test
    @DisplayName("deleteInterview successfully deletes interview when owned")
    void deleteInterview_whenOwned_deletesInterview() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleApplication.setUser(owner);
        sampleInterview.setApplication(sampleApplication);

        when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(sampleInterview));

        interviewService.deleteInterview(1L, 10L);

        verify(interviewRepository).delete(sampleInterview);
    }

    @Test
    @DisplayName("deleteInterview throws 404 when interview missing")
    void deleteInterview_whenMissing_throws404() {
        when(interviewRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());
        when(interviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.deleteInterview(99L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteInterview throws 403 when user is not owner")
    void deleteInterview_whenOtherUser_throws403() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(20L);
        sampleApplication.setUser(owner);
        sampleInterview.setApplication(sampleApplication);

        when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(sampleInterview));

        assertThatThrownBy(() -> interviewService.deleteInterview(1L, 10L))
                .isInstanceOf(AccessDeniedException.class);

        verify(interviewRepository, never()).delete(any());
    }
}
