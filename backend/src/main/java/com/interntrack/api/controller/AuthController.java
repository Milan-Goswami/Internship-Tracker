package com.interntrack.api.controller;

import com.interntrack.api.dto.auth.*;
import com.interntrack.api.entity.User;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.exception.InvalidTokenException;
import com.interntrack.api.exception.InvalidCredentialsException;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.AuthenticationService;
import com.interntrack.api.security.JwtService;
import com.interntrack.api.service.RefreshTokenService;
import com.interntrack.api.service.TokenRotationResult;
import com.interntrack.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.interntrack.api.exception.RateLimitExceededException;
import com.interntrack.api.service.RateLimitService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final long REFRESH_TOKEN_COOKIE_MAX_AGE_SECONDS = 7L * 24 * 60 * 60; // 7 days

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RateLimitService rateLimitService;

    @Value("${security.cookie.secure:false}")
    private boolean cookieSecure;

    public AuthController(
            AuthenticationService authenticationService,
            UserService userService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            RateLimitService rateLimitService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {
            
        String ip = servletRequest.getRemoteAddr();
        if (!rateLimitService.tryConsumeIp("register:ip:" + ip)) {
            throw new RateLimitExceededException("Too many registration attempts. Please try again later.",
                    rateLimitService.getRegisterRetryAfterSeconds());
        }

        AuthenticatedUser registeredPrincipal = authenticationService.register(
                request.email(),
                request.password(),
                request.fullName()
        );
        User user = userService.findById(registeredPrincipal.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserResponse userResponse = UserResponse.from(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "data", userResponse,
                "message", "User registered successfully"
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {

        String ip = servletRequest.getRemoteAddr();
        String normalizedEmail = request.email().trim().toLowerCase();

        // 1. Check IP rate limiter first. A throttled IP is rejected immediately without touching account tokens.
        if (!rateLimitService.tryConsumeIp("login:ip:" + ip)) {
            throw new RateLimitExceededException("Too many login attempts. Please try again later.",
                    rateLimitService.getLoginRetryAfterSeconds());
        }

        // 2. Check account rate limiter
        boolean accountHasTokens = rateLimitService.tryConsumeAccount(normalizedEmail);
        if (!accountHasTokens) {
            // Account bucket is depleted by failed attempts.
            // Check credentials: if legitimate user possesses valid credentials, allow login and clear failure state.
            // If credentials are invalid, enforce 429 throttling to prevent brute-force attacks.
            try {
                AuthenticatedUser authenticatedUser = authenticationService.authenticate(
                        request.email(),
                        request.password()
                );
                rateLimitService.resetAccount(normalizedEmail);
                return buildLoginResponse(authenticatedUser, servletRequest);
            } catch (InvalidCredentialsException e) {
                throw new RateLimitExceededException("Too many login attempts. Please try again later.",
                        rateLimitService.getLoginRetryAfterSeconds());
            }
        }

        // Account has tokens available. Proceed with authentication.
        AuthenticatedUser authenticatedUser = authenticationService.authenticate(
                request.email(),
                request.password()
        );
        // Successful authentication clears the account failure count
        rateLimitService.resetAccount(normalizedEmail);
        return buildLoginResponse(authenticatedUser, servletRequest);
    }

    private ResponseEntity<Map<String, Object>> buildLoginResponse(
            AuthenticatedUser authenticatedUser,
            HttpServletRequest servletRequest) {
        String accessToken = jwtService.generateAccessToken(authenticatedUser);
        User user = userService.findById(authenticatedUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String rawRefreshToken = refreshTokenService.createRefreshToken(user);

        ResponseCookie cookie = createRefreshTokenCookie(rawRefreshToken, REFRESH_TOKEN_COOKIE_MAX_AGE_SECONDS, servletRequest);

        AuthResponse authResponse = AuthResponse.of(
                accessToken,
                900L,
                UserResponse.from(user)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of(
                        "success", true,
                        "data", authResponse,
                        "message", "Authentication successful"
                ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @RequestHeader(value = "X-InternTrack-Client", required = false) String clientHeader,
            @CookieValue(value = "refreshToken", required = false) String cookieToken,
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            HttpServletRequest servletRequest) {

        String ip = servletRequest.getRemoteAddr();
        if (!rateLimitService.tryConsumeIp("refresh:ip:" + ip)) {
            throw new RateLimitExceededException("Too many refresh attempts. Please try again later.",
                    rateLimitService.getRefreshRetryAfterSeconds());
        }


        if (clientHeader == null || !clientHeader.trim().equalsIgnoreCase("web")) {
            throw new AccessDeniedException("Missing or invalid X-InternTrack-Client header");
        }

        String rawRefreshToken = cookieToken;
        if ((rawRefreshToken == null || rawRefreshToken.isBlank()) && requestBody != null) {
            rawRefreshToken = requestBody.refreshToken();
        }

        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }

        TokenRotationResult rotationResult = refreshTokenService.rotateRefreshToken(rawRefreshToken);
        User user = rotationResult.user();
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getIsActive()
        );
        String accessToken = jwtService.generateAccessToken(principal);

        ResponseCookie cookie = createRefreshTokenCookie(rotationResult.rawRefreshToken(), REFRESH_TOKEN_COOKIE_MAX_AGE_SECONDS, servletRequest);

        AuthResponse authResponse = AuthResponse.of(
                accessToken,
                900L,
                UserResponse.from(user)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of(
                        "success", true,
                        "data", authResponse,
                        "message", "Token refreshed successfully"
                ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refreshToken", required = false) String cookieToken,
            HttpServletRequest servletRequest) {

        if (cookieToken != null && !cookieToken.isBlank()) {
            refreshTokenService.revokeRefreshToken(cookieToken);
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser principal) {
            refreshTokenService.revokeAllUserTokens(principal.id());
        }

        ResponseCookie clearingCookie = createRefreshTokenCookie("", 0L, servletRequest);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearingCookie.toString())
                .build();
    }

    private ResponseCookie createRefreshTokenCookie(String token, long maxAgeSeconds, HttpServletRequest request) {
        boolean isSecure = cookieSecure || (request != null && (request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))));
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(isSecure)
                .path("/api/v1/auth")
                .maxAge(maxAgeSeconds)
                .sameSite("Strict")
                .build();
    }
}