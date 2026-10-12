package com.interntrack.api.controller;

import com.interntrack.api.dto.application.ApplicationResponse;
import com.interntrack.api.dto.application.BatchUpdateStatusRequest;
import com.interntrack.api.dto.application.CreateApplicationRequest;
import com.interntrack.api.dto.application.UpdateApplicationRequest;
import com.interntrack.api.dto.application.UpdateApplicationStatusRequest;
import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.enums.ApplicationStatus;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listApplications(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) ApplicationStatus status) {

        List<Application> applications = applicationService.getApplicationsForUser(
                authenticatedUser.id(),
                status
        );

        List<ApplicationResponse> responseList = applications.stream()
                .map(ApplicationResponse::from)
                .toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", responseList,
                "message", "Applications retrieved successfully"
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createApplication(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateApplicationRequest request) {

        Application created = applicationService.createApplication(
                authenticatedUser.id(),
                request.opportunityId(),
                request.status(),
                request.resumeDocumentId(),
                request.coverLetterDocumentId(),
                request.personalNotes(),
                request.userRating()
        );

        URI location = URI.create("/api/v1/applications/" + created.getId());

        return ResponseEntity.created(location).body(Map.of(
                "success", true,
                "data", ApplicationResponse.from(created),
                "message", "Application created successfully"
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getApplicationById(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id) {

        Application application = applicationService.getApplicationById(id, authenticatedUser.id());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", ApplicationResponse.from(application),
                "message", "Application retrieved successfully"
        ));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateApplicationStatusRequest request) {

        Application updated = applicationService.transitionStatus(
                id,
                authenticatedUser.id(),
                request.status()
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", ApplicationResponse.from(updated),
                "message", "Application status updated successfully"
        ));
    }

    @PatchMapping("/batch-status")
    public ResponseEntity<Map<String, Object>> batchUpdateStatus(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody BatchUpdateStatusRequest request) {

        List<Application> updated = applicationService.batchTransitionStatus(
                request.applicationIds(),
                authenticatedUser.id(),
                request.status()
        );

        List<ApplicationResponse> responseList = updated.stream()
                .map(ApplicationResponse::from)
                .toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", responseList,
                "message", "Applications status batch updated successfully"
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateApplication(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateApplicationRequest request) {

        Application updated = applicationService.updateNotesAndRating(
                id,
                authenticatedUser.id(),
                request.personalNotes(),
                request.userRating(),
                request.resumeDocumentId(),
                request.coverLetterDocumentId()
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", ApplicationResponse.from(updated),
                "message", "Application updated successfully"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id) {

        applicationService.deleteApplication(id, authenticatedUser.id());
        return ResponseEntity.noContent().build();
    }
}