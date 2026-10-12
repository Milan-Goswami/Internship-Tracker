package com.interntrack.api.service;

import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.Reminder;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.ReminderType;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.ApplicationRepository;
import com.interntrack.api.repository.ReminderRepository;
import com.interntrack.api.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    public ReminderService(ReminderRepository reminderRepository,
                           UserRepository userRepository,
                           ApplicationRepository applicationRepository) {
        this.reminderRepository = reminderRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    /**
     * Looks up a reminder by its primary key ID.
     */
    @Transactional(readOnly = true)
    public Optional<Reminder> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return reminderRepository.findById(id);
    }

    /**
     * Looks up a reminder by ID and asserts caller ownership.
     */
    @Transactional(readOnly = true)
    public Reminder findAccessibleById(Long reminderId, Long userId) {
        if (reminderId == null) {
            throw new IllegalArgumentException("Reminder ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with id: " + reminderId));

        if (reminder.getUser() == null || !Objects.equals(reminder.getUser().getId(), userId)) {
            throw new AccessDeniedException("Access denied: You do not own this reminder");
        }

        return reminder;
    }

    /**
     * Retrieves all reminders created by a specific user.
     */
    @Transactional(readOnly = true)
    public List<Reminder> findByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return reminderRepository.findByUserIdOrderByDueAtAsc(userId);
    }

    /**
     * Retrieves reminders for a user filtered by completion status, application ID, and/or dueBefore cutoff.
     */
    @Transactional(readOnly = true)
    public List<Reminder> findByUserIdFiltered(Long userId, Boolean completed, Long applicationId, Instant dueBefore) {
        if (userId == null) {
            return List.of();
        }

        if (dueBefore != null) {
            if (applicationId != null && completed != null) {
                return reminderRepository.findByUserIdAndApplicationIdAndCompletedAndDueAtLessThanEqualOrderByDueAtAsc(userId, applicationId, completed, dueBefore);
            } else if (applicationId != null) {
                return reminderRepository.findByUserIdAndApplicationIdAndDueAtLessThanEqualOrderByDueAtAsc(userId, applicationId, dueBefore);
            } else if (completed != null) {
                return reminderRepository.findByUserIdAndCompletedAndDueAtLessThanEqualOrderByDueAtAsc(userId, completed, dueBefore);
            } else {
                return reminderRepository.findByUserIdAndDueAtLessThanEqualOrderByDueAtAsc(userId, dueBefore);
            }
        } else {
            if (applicationId != null && completed != null) {
                return reminderRepository.findByUserIdAndApplicationIdAndCompletedOrderByDueAtAsc(userId, applicationId, completed);
            } else if (applicationId != null) {
                return reminderRepository.findByUserIdAndApplicationIdOrderByDueAtAsc(userId, applicationId);
            } else if (completed != null) {
                return reminderRepository.findByUserIdAndCompletedOrderByDueAtAsc(userId, completed);
            } else {
                return reminderRepository.findByUserIdOrderByDueAtAsc(userId);
            }
        }
    }

    /**
     * Retrieves reminders for a user filtered by completion status and/or application ID (backward compatibility).
     */
    @Transactional(readOnly = true)
    public List<Reminder> findByUserIdFiltered(Long userId, Boolean completed, Long applicationId) {
        return findByUserIdFiltered(userId, completed, applicationId, null);
    }

    /**
     * Retrieves reminders for a user filtered by completion status.
     */
    @Transactional(readOnly = true)
    public List<Reminder> findByUserIdAndCompleted(Long userId, Boolean completed) {
        if (userId == null || completed == null) {
            return List.of();
        }
        return reminderRepository.findByUserIdAndCompletedOrderByDueAtAsc(userId, completed);
    }

    /**
     * Retrieves all incomplete/pending reminders for a user.
     */
    @Transactional(readOnly = true)
    public List<Reminder> findIncompleteByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return reminderRepository.findByUserIdAndCompletedOrderByDueAtAsc(userId, false);
    }

    /**
     * Retrieves reminders linked to a specific application.
     */
    @Transactional(readOnly = true)
    public List<Reminder> findByApplicationId(Long applicationId) {
        if (applicationId == null) {
            return List.of();
        }
        return reminderRepository.findByApplicationId(applicationId);
    }

    /**
     * Creates and persists a new reminder.
     */
    @Transactional
    public Reminder createReminder(
            Long userId,
            Long applicationId,
            String title,
            Instant dueAt,
            ReminderType reminderType) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Reminder title cannot be null or blank");
        }
        if (dueAt == null) {
            throw new IllegalArgumentException("DueAt timestamp cannot be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Application application = null;
        if (applicationId != null) {
            application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

            if (application.getUser() == null || !Objects.equals(application.getUser().getId(), userId)) {
                throw new AccessDeniedException("Access denied: Application belongs to another user");
            }
        }

        Reminder reminder = new Reminder();
        reminder.setUser(user);
        reminder.setApplication(application);
        reminder.setTitle(title.trim());
        reminder.setDueAt(dueAt);
        reminder.setCompleted(false);
        reminder.setReminderType(reminderType != null ? reminderType : ReminderType.CUSTOM);

        return reminderRepository.save(reminder);
    }

    /**
     * Updates mutable fields of an existing reminder.
     */
    @Transactional
    public Reminder updateReminder(
            Long reminderId,
            Long userId,
            Long applicationId,
            String title,
            Instant dueAt,
            Boolean completed,
            ReminderType reminderType) {

        if (reminderId == null) {
            throw new IllegalArgumentException("Reminder ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with id: " + reminderId));

        if (reminder.getUser() == null || !Objects.equals(reminder.getUser().getId(), userId)) {
            throw new AccessDeniedException("Access denied: You do not own this reminder");
        }

        if (applicationId != null) {
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

            if (application.getUser() == null || !Objects.equals(application.getUser().getId(), userId)) {
                throw new AccessDeniedException("Access denied: Application belongs to another user");
            }
            reminder.setApplication(application);
        }

        if (title != null && !title.isBlank()) {
            reminder.setTitle(title.trim());
        }
        if (dueAt != null) {
            reminder.setDueAt(dueAt);
        }
        if (completed != null) {
            reminder.setCompleted(completed);
        }
        if (reminderType != null) {
            reminder.setReminderType(reminderType);
        }

        return reminderRepository.save(reminder);
    }

    /**
     * Marks an existing reminder as completed.
     */
    @Transactional
    public Reminder markCompleted(Long reminderId, Long userId) {
        if (reminderId == null) {
            throw new IllegalArgumentException("Reminder ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with id: " + reminderId));

        if (reminder.getUser() == null || !Objects.equals(reminder.getUser().getId(), userId)) {
            throw new AccessDeniedException("Access denied: You do not own this reminder");
        }

        reminder.setCompleted(true);

        return reminderRepository.save(reminder);
    }

    /**
     * Deletes a reminder belonging to the specified user.
     */
    @Transactional
    public void deleteReminder(Long reminderId, Long userId) {
        if (reminderId == null) {
            throw new IllegalArgumentException("Reminder ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with id: " + reminderId));

        if (reminder.getUser() == null || !Objects.equals(reminder.getUser().getId(), userId)) {
            throw new AccessDeniedException("Access denied: You do not own this reminder");
        }

        reminderRepository.delete(reminder);
    }
}