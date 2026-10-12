package com.interntrack.api.service;

import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.Company;
import com.interntrack.api.entity.Opportunity;
import com.interntrack.api.entity.Reminder;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.ApplicationStatus;
import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.ReminderType;
import com.interntrack.api.entity.enums.SourceProvider;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.ApplicationRepository;
import com.interntrack.api.repository.ReminderRepository;
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
class ReminderServiceTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private ReminderService reminderService;

    private User sampleUser;
    private Application sampleApplication;
    private Reminder sampleReminder;

    @BeforeEach
    void setUp() {
        sampleUser = new User("user@example.com", "hash123", "User One", UserRole.ROLE_USER);
        Company company = new Company("Google", "google.com", "Tech");
        Opportunity opportunity = new Opportunity(company, "SWE Intern", LocationType.REMOTE, SourceProvider.SYSTEM);
        sampleApplication = new Application(sampleUser, opportunity, ApplicationStatus.APPLIED);

        sampleReminder = new Reminder(
                sampleUser,
                sampleApplication,
                "Submit OA Part 1",
                Instant.now().plusSeconds(86400),
                ReminderType.OA_DEADLINE
        );
    }

    @Test
    @DisplayName("findById returns Optional when reminder exists")
    void findById_whenExists_returnsReminder() {
        when(reminderRepository.findById(1L)).thenReturn(Optional.of(sampleReminder));

        Optional<Reminder> result = reminderService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Submit OA Part 1");
    }

    @Test
    @DisplayName("findById returns empty Optional when id is null")
    void findById_whenNull_returnsEmpty() {
        Optional<Reminder> result = reminderService.findById(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(reminderRepository);
    }

    @Test
    @DisplayName("findAccessibleById returns reminder when user is owner")
    void findAccessibleById_whenOwner_returnsReminder() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        sampleReminder.setUser(user);

        when(reminderRepository.findById(1L)).thenReturn(Optional.of(sampleReminder));

        Reminder result = reminderService.findAccessibleById(1L, 10L);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Submit OA Part 1");
    }

    @Test
    @DisplayName("findAccessibleById throws ResourceNotFoundException when reminder missing")
    void findAccessibleById_whenMissing_throwsNotFound() {
        when(reminderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.findAccessibleById(99L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reminder not found with id: 99");
    }

    @Test
    @DisplayName("findAccessibleById throws AccessDeniedException when caller is not owner")
    void findAccessibleById_whenNotOwner_throwsAccessDenied() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleReminder.setUser(owner);

        when(reminderRepository.findById(1L)).thenReturn(Optional.of(sampleReminder));

        assertThatThrownBy(() -> reminderService.findAccessibleById(1L, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("findByUserId returns reminders for given user")
    void findByUserId_returnsUserReminders() {
        when(reminderRepository.findByUserIdOrderByDueAtAsc(1L)).thenReturn(List.of(sampleReminder));

        List<Reminder> results = reminderService.findByUserId(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Submit OA Part 1");
    }

    @Test
    @DisplayName("findByUserId returns empty list when userId is null")
    void findByUserId_whenNull_returnsEmpty() {
        List<Reminder> results = reminderService.findByUserId(null);

        assertThat(results).isEmpty();
        verifyNoInteractions(reminderRepository);
    }

    @Test
    @DisplayName("findByUserIdAndCompleted filters by completion status")
    void findByUserIdAndCompleted_filtersCorrectly() {
        when(reminderRepository.findByUserIdAndCompletedOrderByDueAtAsc(1L, true))
                .thenReturn(List.of());

        List<Reminder> results = reminderService.findByUserIdAndCompleted(1L, true);

        assertThat(results).isEmpty();
        verify(reminderRepository).findByUserIdAndCompletedOrderByDueAtAsc(1L, true);
    }

    @Test
    @DisplayName("findIncompleteByUserId returns only pending reminders")
    void findIncompleteByUserId_returnsOnlyIncomplete() {
        when(reminderRepository.findByUserIdAndCompletedOrderByDueAtAsc(1L, false))
                .thenReturn(List.of(sampleReminder));

        List<Reminder> results = reminderService.findIncompleteByUserId(1L);

        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("findByUserIdFiltered supports combined filters")
    void findByUserIdFiltered_withCompletedAndApplication_callsRepository() {
        when(reminderRepository.findByUserIdAndApplicationIdAndCompletedOrderByDueAtAsc(1L, 5L, false))
                .thenReturn(List.of(sampleReminder));

        List<Reminder> results = reminderService.findByUserIdFiltered(1L, false, 5L);

        assertThat(results).hasSize(1);
        verify(reminderRepository).findByUserIdAndApplicationIdAndCompletedOrderByDueAtAsc(1L, 5L, false);
    }

    @Test
    @DisplayName("findByUserIdFiltered with dueBefore delegates to dueBefore query")
    void findByUserIdFiltered_withDueBefore_callsRepository() {
        Instant cutoff = Instant.now().plusSeconds(3600);
        when(reminderRepository.findByUserIdAndDueAtLessThanEqualOrderByDueAtAsc(1L, cutoff))
                .thenReturn(List.of(sampleReminder));

        List<Reminder> results = reminderService.findByUserIdFiltered(1L, null, null, cutoff);

        assertThat(results).hasSize(1);
        verify(reminderRepository).findByUserIdAndDueAtLessThanEqualOrderByDueAtAsc(1L, cutoff);
    }

    @Test
    @DisplayName("findByUserIdFiltered with dueBefore and completed delegates to matching query")
    void findByUserIdFiltered_withDueBeforeAndCompleted_callsRepository() {
        Instant cutoff = Instant.now().plusSeconds(3600);
        when(reminderRepository.findByUserIdAndCompletedAndDueAtLessThanEqualOrderByDueAtAsc(1L, false, cutoff))
                .thenReturn(List.of(sampleReminder));

        List<Reminder> results = reminderService.findByUserIdFiltered(1L, false, null, cutoff);

        assertThat(results).hasSize(1);
        verify(reminderRepository).findByUserIdAndCompletedAndDueAtLessThanEqualOrderByDueAtAsc(1L, false, cutoff);
    }

    @Test
    @DisplayName("findByUserIdFiltered with dueBefore and application delegates to matching query")
    void findByUserIdFiltered_withDueBeforeAndApplication_callsRepository() {
        Instant cutoff = Instant.now().plusSeconds(3600);
        when(reminderRepository.findByUserIdAndApplicationIdAndDueAtLessThanEqualOrderByDueAtAsc(1L, 5L, cutoff))
                .thenReturn(List.of(sampleReminder));

        List<Reminder> results = reminderService.findByUserIdFiltered(1L, null, 5L, cutoff);

        assertThat(results).hasSize(1);
        verify(reminderRepository).findByUserIdAndApplicationIdAndDueAtLessThanEqualOrderByDueAtAsc(1L, 5L, cutoff);
    }

    @Test
    @DisplayName("findByUserIdFiltered with dueBefore, completed, and application delegates to matching query")
    void findByUserIdFiltered_withDueBeforeCompletedAndApplication_callsRepository() {
        Instant cutoff = Instant.now().plusSeconds(3600);
        when(reminderRepository.findByUserIdAndApplicationIdAndCompletedAndDueAtLessThanEqualOrderByDueAtAsc(1L, 5L, false, cutoff))
                .thenReturn(List.of(sampleReminder));

        List<Reminder> results = reminderService.findByUserIdFiltered(1L, false, 5L, cutoff);

        assertThat(results).hasSize(1);
        verify(reminderRepository).findByUserIdAndApplicationIdAndCompletedAndDueAtLessThanEqualOrderByDueAtAsc(1L, 5L, false, cutoff);
    }

    @Test
    @DisplayName("findByApplicationId returns reminders linked to given application")
    void findByApplicationId_returnsReminders() {
        when(reminderRepository.findByApplicationId(5L)).thenReturn(List.of(sampleReminder));

        List<Reminder> results = reminderService.findByApplicationId(5L);

        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("createReminder successfully creates and persists custom reminder")
    void createReminder_createsAndSaves() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant due = Instant.now().plusSeconds(3600);
        Reminder created = reminderService.createReminder(
                1L,
                null,
                "Check portal for status",
                due,
                ReminderType.CUSTOM
        );

        assertThat(created).isNotNull();
        assertThat(created.getUser()).isEqualTo(sampleUser);
        assertThat(created.getApplication()).isNull();
        assertThat(created.getTitle()).isEqualTo("Check portal for status");
        assertThat(created.getDueAt()).isEqualTo(due);
        assertThat(created.getCompleted()).isFalse();
        assertThat(created.getReminderType()).isEqualTo(ReminderType.CUSTOM);

        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Check portal for status");

        verifyNoInteractions(applicationRepository);
    }

    @Test
    @DisplayName("createReminder successfully creates reminder linked to application")
    void createReminder_withApplication_createsAndSaves() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        sampleApplication.setUser(user);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(sampleApplication));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant due = Instant.now().plusSeconds(7200);
        Reminder created = reminderService.createReminder(
                10L,
                5L,
                "Follow up with recruiter",
                due,
                ReminderType.FOLLOW_UP
        );

        assertThat(created).isNotNull();
        assertThat(created.getApplication()).isEqualTo(sampleApplication);
        assertThat(created.getReminderType()).isEqualTo(ReminderType.FOLLOW_UP);
    }

    @Test
    @DisplayName("createReminder throws ResourceNotFoundException when user missing")
    void createReminder_whenUserMissing_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.createReminder(
                99L, null, "Title", Instant.now(), ReminderType.CUSTOM
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(reminderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createReminder throws ResourceNotFoundException when application missing")
    void createReminder_whenApplicationMissing_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.createReminder(
                1L, 999L, "Title", Instant.now(), ReminderType.CUSTOM
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found");

        verify(reminderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createReminder throws AccessDeniedException when application belongs to another user")
    void createReminder_whenApplicationBelongsToOtherUser_throwsException() {
        User otherUser = mock(User.class);
        when(otherUser.getId()).thenReturn(99L);
        sampleApplication.setUser(otherUser);

        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(sampleApplication));

        // User 10 tries to link Application owned by User 99
        assertThatThrownBy(() -> reminderService.createReminder(
                10L, 5L, "Title", Instant.now(), ReminderType.CUSTOM
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(reminderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createReminder throws IllegalArgumentException on blank title or missing dueAt")
    void createReminder_whenInvalidInputs_throwsException() {
        assertThatThrownBy(() -> reminderService.createReminder(
                1L, null, "   ", Instant.now(), ReminderType.CUSTOM
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> reminderService.createReminder(
                1L, null, "Valid Title", null, ReminderType.CUSTOM
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateReminder updates fields when requested by owner")
    void updateReminder_whenOwner_updatesFields() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        sampleReminder.setUser(user);

        when(reminderRepository.findById(1L)).thenReturn(Optional.of(sampleReminder));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant newDue = Instant.now().plusSeconds(10000);
        Reminder updated = reminderService.updateReminder(
                1L,
                10L,
                null,
                "Updated Title",
                newDue,
                true,
                ReminderType.INTERVIEW_PREP
        );

        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getDueAt()).isEqualTo(newDue);
        assertThat(updated.getCompleted()).isTrue();
        assertThat(updated.getReminderType()).isEqualTo(ReminderType.INTERVIEW_PREP);
    }

    @Test
    @DisplayName("updateReminder throws AccessDeniedException when non-owner requests update")
    void updateReminder_whenNonOwner_throwsException() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleReminder.setUser(owner);

        when(reminderRepository.findById(1L)).thenReturn(Optional.of(sampleReminder));

        // User 99 tries to update User 10's reminder
        assertThatThrownBy(() -> reminderService.updateReminder(
                1L, 99L, null, "Hacked Title", null, null, null
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(reminderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateReminder throws AccessDeniedException when linking application of another user")
    void updateReminder_whenOtherUsersApplication_throwsAccessDenied() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleReminder.setUser(owner);

        User otherUser = mock(User.class);
        when(otherUser.getId()).thenReturn(99L);
        sampleApplication.setUser(otherUser);

        when(reminderRepository.findById(1L)).thenReturn(Optional.of(sampleReminder));
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(sampleApplication));

        assertThatThrownBy(() -> reminderService.updateReminder(
                1L, 10L, 5L, "Title", null, null, null
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied: Application belongs to another user");

        verify(reminderRepository, never()).save(any());
    }

    @Test
    @DisplayName("markCompleted sets completed=true when requested by owner")
    void markCompleted_whenOwner_marksCompleted() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        sampleReminder.setUser(user);
        sampleReminder.setCompleted(false);

        when(reminderRepository.findById(1L)).thenReturn(Optional.of(sampleReminder));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reminder completed = reminderService.markCompleted(1L, 10L);

        assertThat(completed.getCompleted()).isTrue();
    }

    @Test
    @DisplayName("markCompleted throws AccessDeniedException when requested by non-owner")
    void markCompleted_whenNonOwner_throwsException() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleReminder.setUser(owner);

        when(reminderRepository.findById(1L)).thenReturn(Optional.of(sampleReminder));

        assertThatThrownBy(() -> reminderService.markCompleted(1L, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(reminderRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteReminder deletes reminder when requested by owner")
    void deleteReminder_whenOwner_deletesReminder() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        sampleReminder.setUser(user);

        when(reminderRepository.findById(1L)).thenReturn(Optional.of(sampleReminder));

        reminderService.deleteReminder(1L, 10L);

        verify(reminderRepository).delete(sampleReminder);
    }

    @Test
    @DisplayName("deleteReminder throws AccessDeniedException when requested by non-owner")
    void deleteReminder_whenNonOwner_throwsException() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleReminder.setUser(owner);

        when(reminderRepository.findById(1L)).thenReturn(Optional.of(sampleReminder));

        assertThatThrownBy(() -> reminderService.deleteReminder(1L, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(reminderRepository, never()).delete(any());
    }
}