package com.interntrack.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interntrack.api.dto.auth.LoginRequest;
import com.interntrack.api.dto.auth.RefreshTokenRequest;
import com.interntrack.api.dto.auth.RegisterRequest;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.AccountInactiveException;
import com.interntrack.api.exception.InvalidCredentialsException;
import com.interntrack.api.exception.InvalidTokenException;
import com.interntrack.api.exception.UserAlreadyExistsException;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.AuthenticationService;
import com.interntrack.api.security.JwtService;
import com.interntrack.api.service.RefreshTokenService;
import com.interntrack.api.service.TokenRotationResult;
import com.interntrack.api.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=TEST_AUTH_CONTROLLER_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    private MockMvc mockMvc;
    private User sampleUser;
    private AuthenticatedUser samplePrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        sampleUser = new User("candidate@example.com", "", "Alice Candidate", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(sampleUser, "id", 101L);
        sampleUser.setIsActive(true);

        samplePrincipal = new AuthenticatedUser(101L, "candidate@example.com", "Alice Candidate", UserRole.ROLE_USER, true);
    }

    // --- REGISTRATION ---

    @Test
    @DisplayName("POST /api/v1/auth/register with valid payload returns 201 Created and safe UserResponse")
    void register_validPayload_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest("candidate@example.com", "Password@123", "Alice Candidate");

        when(authenticationService.register(eq("candidate@example.com"), eq("Password@123"), eq("Alice Candidate")))
                .thenReturn(samplePrincipal);
        when(userService.findById(101L)).thenReturn(Optional.of(sampleUser));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.email").value("candidate@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Alice Candidate"))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with duplicate email returns 409 Conflict")
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest("existing@example.com", "Password@123", "Duplicate User");

        when(authenticationService.register(any(), any(), any()))
                .thenThrow(new UserAlreadyExistsException("User with email already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with invalid email returns 400 Bad Request")
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("invalid-email", "Password@123", "Alice Candidate");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with short password returns 400 Bad Request")
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("candidate@example.com", "short", "Alice Candidate");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with blank full name returns 400 Bad Request")
    void register_blankFullName_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("candidate@example.com", "Password@123", "   ");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // --- LOGIN ---

    @Test
    @DisplayName("POST /api/v1/auth/login with valid credentials returns 200 OK, sets HttpOnly cookie, and hides refresh token in JSON")
    void login_validCredentials_returns200() throws Exception {
        LoginRequest request = new LoginRequest("candidate@example.com", "Password@123");

        when(authenticationService.authenticate("candidate@example.com", "Password@123"))
                .thenReturn(samplePrincipal);
        when(jwtService.generateAccessToken(samplePrincipal)).thenReturn("mocked.access.jwt.token");
        when(userService.findById(101L)).thenReturn(Optional.of(sampleUser));
        when(refreshTokenService.createRefreshToken(sampleUser)).thenReturn("mocked_raw_refresh_token_123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=mocked_raw_refresh_token_123")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mocked.access.jwt.token"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.user.email").value("candidate@example.com"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with invalid credentials returns 401 Unauthorized")
    void login_invalidCredentials_returns401() throws Exception {
        LoginRequest request = new LoginRequest("candidate@example.com", "WrongPassword");

        when(authenticationService.authenticate(any(), any()))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with inactive account returns 403 Forbidden")
    void login_inactiveAccount_returns403() throws Exception {
        LoginRequest request = new LoginRequest("inactive@example.com", "Password@123");

        when(authenticationService.authenticate(any(), any()))
                .thenThrow(new AccountInactiveException("Account is deactivated"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // --- REFRESH ---

    @Test
    @DisplayName("POST /api/v1/auth/refresh with valid cookie and X-InternTrack-Client header rotates token and sets new cookie")
    void refresh_validCookie_returns200() throws Exception {
        TokenRotationResult rotationResult = new TokenRotationResult(sampleUser, "new_raw_refresh_token_456");
        when(refreshTokenService.rotateRefreshToken("valid_raw_refresh_token")).thenReturn(rotationResult);
        when(jwtService.generateAccessToken(any(AuthenticatedUser.class))).thenReturn("new.mocked.access.jwt");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-InternTrack-Client", "web")
                        .cookie(new Cookie("refreshToken", "valid_raw_refresh_token")))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=new_raw_refresh_token_456")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new.mocked.access.jwt"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh missing X-InternTrack-Client header returns 403 Forbidden")
    void refresh_missingHeader_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", "valid_raw_refresh_token")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh missing refresh token returns 401 Unauthorized")
    void refresh_missingToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-InternTrack-Client", "web"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh with invalid/expired/revoked token returns 401 Unauthorized")
    void refresh_invalidToken_returns401() throws Exception {
        when(refreshTokenService.rotateRefreshToken("revoked_or_invalid_token"))
                .thenThrow(new InvalidTokenException("Invalid refresh token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-InternTrack-Client", "web")
                        .cookie(new Cookie("refreshToken", "revoked_or_invalid_token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    // --- LOGOUT ---

    @Test
    @DisplayName("POST /api/v1/auth/logout with cookie revokes token, clears cookie with Max-Age=0, and returns 204 No Content")
    void logout_withCookie_returns204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("refreshToken", "active_raw_token_to_revoke")))
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")));

        verify(refreshTokenService).revokeRefreshToken("active_raw_token_to_revoke");
    }
}