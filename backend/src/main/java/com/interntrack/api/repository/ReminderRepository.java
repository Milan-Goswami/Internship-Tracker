package com.interntrack.api.repository;

import com.interntrack.api.entity.Reminder;
import com.interntrack.api.entity.enums.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByUserIdOrderByDueAtAsc(Long userId);

    List<Reminder> findByUserId(Long userId);

    List<Reminder> findByUserIdAndCompletedOrderByDueAtAsc(Long userId, Boolean completed);

    List<Reminder> findByUserIdAndCompleted(Long userId, Boolean completed);

    List<Reminder> findByUserIdAndApplicationIdOrderByDueAtAsc(Long userId, Long applicationId);

    List<Reminder> findByUserIdAndApplicationIdAndCompletedOrderByDueAtAsc(Long userId, Long applicationId, Boolean completed);

    List<Reminder> findByApplicationId(Long applicationId);

    // dueBefore filter variants (dueAt <= dueBefore)
    List<Reminder> findByUserIdAndDueAtLessThanEqualOrderByDueAtAsc(Long userId, java.time.Instant dueBefore);

    List<Reminder> findByUserIdAndCompletedAndDueAtLessThanEqualOrderByDueAtAsc(Long userId, Boolean completed, java.time.Instant dueBefore);

    List<Reminder> findByUserIdAndApplicationIdAndDueAtLessThanEqualOrderByDueAtAsc(Long userId, Long applicationId, java.time.Instant dueBefore);

    List<Reminder> findByUserIdAndApplicationIdAndCompletedAndDueAtLessThanEqualOrderByDueAtAsc(Long userId, Long applicationId, Boolean completed, java.time.Instant dueBefore);

    long countByUserIdAndCompletedFalseAndReminderType(Long userId, ReminderType reminderType);

    List<Reminder> findTop5ByUserIdAndCompletedFalseOrderByDueAtAsc(Long userId);
}