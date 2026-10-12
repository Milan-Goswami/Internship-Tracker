package com.interntrack.api.dto.reminder;

import com.interntrack.api.entity.enums.ReminderType;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateReminderRequest(
        @Size(max = 255, message = "Reminder title cannot exceed 255 characters")
        String title,

        Instant dueAt,

        Boolean completed,

        ReminderType reminderType,

        Long applicationId
) {
}