package com.interntrack.api.controller;

import com.interntrack.api.dto.analytics.DashboardSummaryResponse;
import com.interntrack.api.dto.analytics.FunnelAnalyticsResponse;
import com.interntrack.api.dto.skill.SkillDeficitsResponse;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        DashboardSummaryResponse summary = analyticsService.getDashboardSummary(authenticatedUser.id());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", summary,
                "message", "Dashboard summary retrieved successfully"
        ));
    }

    @GetMapping("/funnel")
    public ResponseEntity<Map<String, Object>> getFunnelAnalytics(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        FunnelAnalyticsResponse funnel = analyticsService.getFunnelAnalytics(authenticatedUser.id());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", funnel,
                "message", "Funnel analytics retrieved successfully"
        ));
    }

    @GetMapping("/skill-deficits")
    public ResponseEntity<Map<String, Object>> getSkillDeficits(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        SkillDeficitsResponse deficits = analyticsService.getSkillDeficits(authenticatedUser.id());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", deficits,
                "message", "Skill deficits retrieved successfully"
        ));
    }
}