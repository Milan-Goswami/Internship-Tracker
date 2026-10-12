package com.interntrack.api.security;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Test-only REST controller located exclusively in test source set
 * to verify Spring Security filter chains and role authorizations.
 */
@RestController
@RequestMapping("/api/v1/test")
public class TestSecurityController {

    @GetMapping("/protected")
    public ResponseEntity<Map<String, Object>> protectedEndpoint(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "userId", user.id(),
                "email", user.email(),
                "role", user.role().name()
        ));
    }

    @GetMapping("/admin-only")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> adminOnlyEndpoint() {
        return ResponseEntity.ok(Map.of("status", "admin-access-granted"));
    }
}