package com.interntrack.api.controller;

import com.interntrack.api.dto.opportunity.CreateOpportunityRequest;
import com.interntrack.api.dto.opportunity.OpportunityResponse;
import com.interntrack.api.dto.opportunity.UpdateOpportunityRequest;
import com.interntrack.api.dto.skill.OpportunitySkillMatchResponse;
import com.interntrack.api.entity.Opportunity;
import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.SourceProvider;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.service.OpportunityService;
import com.interntrack.api.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/opportunities")
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final SkillService skillService;

    public OpportunityController(OpportunityService opportunityService, SkillService skillService) {
        this.opportunityService = opportunityService;
        this.skillService = skillService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listAccessibleOpportunities(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) LocationType locationType,
            @RequestParam(required = false) String search) {

        List<Opportunity> opportunities = opportunityService.findAccessibleOpportunities(
                authenticatedUser.id(),
                locationType,
                search
        );

        List<OpportunityResponse> responseList = opportunities.stream()
                .map(OpportunityResponse::from)
                .toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", responseList,
                "message", "Opportunities retrieved successfully"
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOpportunityById(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id) {

        Opportunity opportunity = opportunityService.findAccessibleById(id, authenticatedUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + id));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", OpportunityResponse.from(opportunity),
                "message", "Opportunity retrieved successfully"
        ));
    }

    @GetMapping("/{id}/skill-match")
    public ResponseEntity<Map<String, Object>> getOpportunitySkillMatch(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id) {

        OpportunitySkillMatchResponse match = skillService.getOpportunitySkillMatch(authenticatedUser.id(), id);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", match,
                "message", "Skill match computed successfully"
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOpportunity(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateOpportunityRequest request) {

        Opportunity created = opportunityService.createOpportunity(
                request.companyId(),
                authenticatedUser.id(),
                SourceProvider.USER_MANUAL,
                null,
                request.title(),
                request.description(),
                request.locationType(),
                request.locationCity(),
                request.stipendAmount(),
                request.stipendCurrency(),
                request.stipendPeriod(),
                request.durationMonths(),
                request.applicationDeadline(),
                request.sourceUrl(),
                null
        );

        URI location = URI.create("/api/v1/opportunities/" + created.getId());

        return ResponseEntity.created(location).body(Map.of(
                "success", true,
                "data", OpportunityResponse.from(created),
                "message", "Opportunity created successfully"
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateOpportunity(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateOpportunityRequest request) {

        Opportunity updated = opportunityService.updateOpportunity(
                id,
                authenticatedUser.id(),
                request.title(),
                request.description(),
                request.locationType(),
                request.locationCity(),
                request.stipendAmount(),
                request.stipendCurrency(),
                request.stipendPeriod(),
                request.durationMonths(),
                request.applicationDeadline(),
                request.sourceUrl(),
                null // status is system-controlled
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", OpportunityResponse.from(updated),
                "message", "Opportunity updated successfully"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOpportunity(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id) {

        opportunityService.deleteOpportunity(id, authenticatedUser.id());
        return ResponseEntity.noContent().build();
    }
}