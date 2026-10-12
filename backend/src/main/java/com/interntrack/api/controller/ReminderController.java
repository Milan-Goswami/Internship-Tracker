package com.interntrack.api.controller;

import com.interntrack.api.dto.reminder.CreateReminderRequest;
import com.interntrack.api.dto.reminder.ReminderResponse;
import com.interntrack.api.dto.reminder.UpdateReminderRequest;
import com.interntrack.api.entity.Reminder;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.service.ReminderService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listReminders(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueBefore) {

        List<Reminder> reminders = reminderService.findByUserIdFiltered(
                authenticatedUser.id(),
                completed,
                applicationId,
                dueBefore
        );

        List<ReminderResponse> responseList = reminders.stream()
                .map(ReminderResponse::from)
                .toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", responseList,
                "message", "Reminders retrieved successfully"
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createReminder(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateReminderRequest request) {

        Reminder created = reminderService.createReminder(
                authenticatedUser.id(),
                request.applicationId(),
                request.title(),
                request.dueAt(),
                request.reminderType()
        );

        URI location = URI.create("/api/v1/reminders/" + created.getId());

        return ResponseEntity.created(location).body(Map.of(
                "success", true,
                "data", ReminderResponse.from(created),
                "message", "Reminder created successfully"
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getReminderById(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id) {

        Reminder reminder = reminderService.findAccessibleById(id, authenticatedUser.id());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", ReminderResponse.from(reminder),
                "message", "Reminder retrieved successfully"
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateReminder(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateReminderRequest request) {

        Reminder updated = reminderService.updateReminder(
                id,
                authenticatedUser.id(),
                request.applicationId(),
                request.title(),
                request.dueAt(),
                request.completed(),
                request.reminderType()
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", ReminderResponse.from(updated),
                "message", "Reminder updated successfully"
        ));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Map<String, Object>> completeReminder(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id) {

        Reminder completed = reminderService.markCompleted(id, authenticatedUser.id());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", ReminderResponse.from(completed),
                "message", "Reminder marked as completed"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReminder(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id) {

        reminderService.deleteReminder(id, authenticatedUser.id());
        return ResponseEntity.noContent().build();
    }
}