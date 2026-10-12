package com.interntrack.api.dto.reminder;

import com.interntrack.api.entity.enums.ReminderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateReminderRequest(
        @NotBlank(message = "Reminder title cannot be blank")
        @Size(max = 255, message = "Reminder title cannot exceed 255 characters")
        String title,

        @NotNull(message = "Due date timestamp is required")
        Instant dueAt,

        ReminderType reminderType,

        Long applicationId
) {
}