package com.interntrack.api.dto.reminder;

import com.interntrack.api.entity.Reminder;
import com.interntrack.api.entity.enums.ReminderType;

import java.time.Instant;

public record ReminderResponse(
        Long id,
        Long applicationId,
        String title,
        Instant dueAt,
        Boolean completed,
        ReminderType reminderType,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReminderResponse from(Reminder reminder) {
        if (reminder == null) {
            return null;
        }

        Long appId = reminder.getApplication() != null ? reminder.getApplication().getId() : null;

        return new ReminderResponse(
                reminder.getId(),
                appId,
                reminder.getTitle(),
                reminder.getDueAt(),
                reminder.getCompleted(),
                reminder.getReminderType(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt()
        );
    }
}