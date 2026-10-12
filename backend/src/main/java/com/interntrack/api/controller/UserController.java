package com.interntrack.api.controller;

import com.interntrack.api.dto.user.UpdateUserProfilePreferencesRequest;
import com.interntrack.api.dto.user.UpdateUserProfileRequest;
import com.interntrack.api.dto.user.UserProfileResponse;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        UserProfileResponse response = userService.getUserProfile(authenticatedUser.id());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response,
                "message", "User profile retrieved successfully"
        ));
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateProfileAndPreferences(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateUserProfilePreferencesRequest request) {

        UserProfileResponse response = userService.updatePreferences(authenticatedUser.id(), request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response,
                "message", "User profile and preferences updated successfully"
        ));
    }

    @PatchMapping("/me")
    public ResponseEntity<Map<String, Object>> updateMe(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateUserProfileRequest request) {

        UserProfileResponse response = userService.updateProfile(authenticatedUser.id(), request.fullName());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response,
                "message", "User profile updated successfully"
        ));
    }
}