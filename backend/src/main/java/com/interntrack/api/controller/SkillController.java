package com.interntrack.api.controller;

import com.interntrack.api.dto.skill.AddUserSkillRequest;
import com.interntrack.api.dto.skill.SkillResponse;
import com.interntrack.api.dto.skill.UserSkillResponse;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchSkills(@RequestParam(value = "q", required = false) String query) {
        List<SkillResponse> results = skillService.searchSkills(query);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", results,
                "message", "Skills retrieved successfully"
        ));
    }

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getUserSkills(@AuthenticationPrincipal AuthenticatedUser user) {
        List<UserSkillResponse> skills = skillService.getUserSkills(user.id());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", skills,
                "message", "User skills retrieved successfully"
        ));
    }

    @PostMapping("/user")
    public ResponseEntity<Map<String, Object>> addUserSkill(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AddUserSkillRequest request) {

        UserSkillResponse created = skillService.addUserSkill(user.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "data", created,
                "message", "Skill added to user profile successfully"
        ));
    }

    @DeleteMapping("/user/{skillId}")
    public ResponseEntity<Void> removeUserSkill(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long skillId) {

        skillService.removeUserSkill(user.id(), skillId);
        return ResponseEntity.noContent().build();
    }
}