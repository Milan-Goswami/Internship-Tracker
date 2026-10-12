package com.interntrack.api.controller;

import com.interntrack.api.dto.interview.CreateInterviewRequest;
import com.interntrack.api.dto.interview.InterviewResponse;
import com.interntrack.api.dto.interview.UpdateInterviewOutcomeRequest;
import com.interntrack.api.dto.interview.UpdateInterviewRequest;
import com.interntrack.api.entity.Interview;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @GetMapping("/applications/{appId}/interviews")
    public ResponseEntity<Map<String, Object>> listInterviews(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long appId) {

        List<Interview> interviews = interviewService.getInterviewsForApplication(appId, authenticatedUser.id());
        List<InterviewResponse> responses = interviews.stream()
                .map(InterviewResponse::from)
                .toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", responses,
                "message", "Interviews retrieved successfully"
        ));
    }

    @PostMapping("/applications/{appId}/interviews")
    public ResponseEntity<Map<String, Object>> createInterview(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long appId,
            @Valid @RequestBody CreateInterviewRequest request) {

        Interview created = interviewService.createInterview(
                appId,
                authenticatedUser.id(),
                request.roundNumber(),
                request.roundTitle(),
                request.roundType(),
                request.scheduledAt(),
                request.durationMinutes(),
                request.interviewerNames(),
                request.interviewerEmails(),
                request.prepNotes()
        );

        return ResponseEntity
                .created(URI.create("/api/v1/interviews/" + created.getId()))
                .body(Map.of(
                        "success", true,
                        "data", InterviewResponse.from(created),
                        "message", "Interview scheduled successfully"
                ));
    }

    @GetMapping("/interviews/{interviewId}")
    public ResponseEntity<Map<String, Object>> getInterview(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long interviewId) {

        Interview interview = interviewService.getInterviewById(interviewId, authenticatedUser.id());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", InterviewResponse.from(interview),
                "message", "Interview retrieved successfully"
        ));
    }

    @PutMapping("/interviews/{interviewId}")
    public ResponseEntity<Map<String, Object>> updateInterview(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long interviewId,
            @Valid @RequestBody UpdateInterviewRequest request) {

        Interview updated = interviewService.updateInterview(
                interviewId,
                authenticatedUser.id(),
                request.roundNumber(),
                request.roundTitle(),
                request.roundType(),
                request.scheduledAt(),
                request.durationMinutes(),
                request.interviewerNames(),
                request.interviewerEmails(),
                request.prepNotes(),
                request.reflectionNotes(),
                request.outcome()
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", InterviewResponse.from(updated),
                "message", "Interview updated successfully"
        ));
    }

    @PatchMapping("/interviews/{interviewId}/outcome")
    public ResponseEntity<Map<String, Object>> recordOutcome(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long interviewId,
            @Valid @RequestBody UpdateInterviewOutcomeRequest request) {

        Interview updated = interviewService.recordOutcome(
                interviewId,
                authenticatedUser.id(),
                request.outcome(),
                request.reflectionNotes()
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", InterviewResponse.from(updated),
                "message", "Interview outcome recorded successfully"
        ));
    }

    @DeleteMapping("/interviews/{interviewId}")
    public ResponseEntity<Void> deleteInterview(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long interviewId) {

        interviewService.deleteInterview(interviewId, authenticatedUser.id());
        return ResponseEntity.noContent().build();
    }
}
