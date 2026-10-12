package com.interntrack.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interntrack.api.dto.user.UpdateUserProfileRequest;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.JwtService;
import com.interntrack.api.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import com.interntrack.api.dto.user.UserProfileResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=TEST_USER_CONTROLLER_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class UserControllerTest {

    private static final String TEST_SECRET = "TEST_USER_CONTROLLER_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    private MockMvc mockMvc;
    private User sampleUser;
    private AuthenticatedUser samplePrincipal;
    private String validToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        sampleUser = new User("candidate@example.com", "$2a$12$someHashedPasswordNeverExposed", "Alice Candidate", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(sampleUser, "id", 101L);
        sampleUser.setIsActive(true);

        samplePrincipal = new AuthenticatedUser(101L, "candidate@example.com", "Alice Candidate", UserRole.ROLE_USER, true);
        validToken = jwtService.generateAccessToken(samplePrincipal);
    }

    // --- GET /api/v1/users/me ---

    @Test
    @DisplayName("GET /api/v1/users/me without Authorization header returns 401 Unauthorized")
    void getMe_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me with expired JWT returns 401 Unauthorized")
    void getMe_withExpiredToken_returns401() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("101")
                .claim("email", "candidate@example.com")
                .claim("role", "ROLE_USER")
                .issuedAt(new Date(System.currentTimeMillis() - 200_000))
                .expiration(new Date(System.currentTimeMillis() - 100_000))
                .signWith(key, Jwts.SIG.HS512)
                .compact();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/users/me with malformed JWT returns 401 Unauthorized")
    void getMe_withMalformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer invalid.malformed.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/users/me with valid JWT returns current user profile and 200 OK")
    void getMe_withValidToken_returnsCurrentUser() throws Exception {
        when(userService.getUserProfile(101L)).thenReturn(UserProfileResponse.from(sampleUser));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.email").value("candidate@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Alice Candidate"))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/users/me never exposes password or passwordHash in payload")
    void getMe_doesNotExposePasswordHash() throws Exception {
        when(userService.getUserProfile(101L)).thenReturn(UserProfileResponse.from(sampleUser));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/users/me does not expose secrets, tokens or internal JPA references")
    void getMe_doesNotExposeSecuritySecrets() throws Exception {
        when(userService.getUserProfile(101L)).thenReturn(UserProfileResponse.from(sampleUser));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.tokenHash").doesNotExist())
                .andExpect(jsonPath("$.data.jwtSecret").doesNotExist());
    }

    // --- PATCH /api/v1/users/me ---

    @Test
    @DisplayName("PATCH /api/v1/users/me without Authorization header returns 401 Unauthorized")
    void updateMe_withoutToken_returns401() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("Alice New");

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me with valid JWT updates allowed mutable fields and returns 200 OK")
    void updateMe_withValidToken_updatesAllowedFields() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("Alice Updated");

        User updatedUser = new User("candidate@example.com", "$2a$12$hash", "Alice Updated", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(updatedUser, "id", 101L);
        updatedUser.setIsActive(true);

        when(userService.updateProfile(eq(101L), eq("Alice Updated"))).thenReturn(UserProfileResponse.from(updatedUser));

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.fullName").value("Alice Updated"));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me with malicious mass-assignment payload only mutates fullName and preserves all security fields")
    void updateMe_maliciousMassAssignment_onlyMutatesFullName() throws Exception {
        String maliciousPayload = "{" +
                "\"id\": 999999," +
                "\"email\": \"attacker@example.com\"," +
                "\"fullName\": \"Changed Name\"," +
                "\"role\": \"ROLE_ADMIN\"," +
                "\"isActive\": true," +
                "\"createdAt\": \"2000-01-01T00:00:00Z\"," +
                "\"updatedAt\": \"2000-01-01T00:00:00Z\"," +
                "\"passwordHash\": \"malicious\"" +
                "}";

        User safeUser = new User("candidate@example.com", "$2a$12$hash", "Changed Name", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(safeUser, "id", 101L);
        safeUser.setIsActive(true);

        when(userService.updateProfile(eq(101L), eq("Changed Name"))).thenReturn(UserProfileResponse.from(safeUser));

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.email").value("candidate@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Changed Name"))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me with invalid payload (blank fullName) returns 400 Bad Request")
    void updateMe_invalidPayload_returns400() throws Exception {
        UpdateUserProfileRequest invalidRequest = new UpdateUserProfileRequest("   ");

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("User identity always resolves from SecurityContext, preventing IDOR / modifying other users")
    void userCannotUpdateAnotherUser() throws Exception {
        AuthenticatedUser userBob = new AuthenticatedUser(202L, "bob@example.com", "Bob", UserRole.ROLE_USER, true);
        String bobToken = jwtService.generateAccessToken(userBob);

        User bobEntity = new User("bob@example.com", "$2a$12$hash", "Bob Updated", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(bobEntity, "id", 202L);

        when(userService.updateProfile(eq(202L), eq("Bob Updated"))).thenReturn(UserProfileResponse.from(bobEntity));

        // Bob's token will only update Bob (202L)
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserProfileRequest("Bob Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(202))
                .andExpect(jsonPath("$.data.fullName").value("Bob Updated"));
    }

    // --- PUT /api/v1/users/me Tests ---

    @Test
    @DisplayName("PUT /api/v1/users/me without token returns 401 Unauthorized")
    void updateProfileAndPreferences_withoutToken_returns401() throws Exception {
        com.interntrack.api.dto.user.UpdateUserProfilePreferencesRequest request = new com.interntrack.api.dto.user.UpdateUserProfilePreferencesRequest(
                "Alice", "SWE Intern", "Seattle, WA", java.math.BigDecimal.valueOf(45), java.math.BigDecimal.valueOf(65), com.interntrack.api.entity.enums.PreferredWorkMode.HYBRID
        );

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/users/me with valid payload returns 200 OK with preferences")
    void updateProfileAndPreferences_valid_returns200() throws Exception {
        com.interntrack.api.dto.user.UpdateUserProfilePreferencesRequest request = new com.interntrack.api.dto.user.UpdateUserProfilePreferencesRequest(
                "Alice Updated", "SWE Intern", "Seattle, WA", java.math.BigDecimal.valueOf(45), java.math.BigDecimal.valueOf(65), com.interntrack.api.entity.enums.PreferredWorkMode.HYBRID
        );
        com.interntrack.api.dto.user.UserProfileResponse resp = new com.interntrack.api.dto.user.UserProfileResponse(
                101L, "alice@example.com", "Alice Updated", UserRole.ROLE_USER, true,
                "SWE Intern", "Seattle, WA", java.math.BigDecimal.valueOf(45), java.math.BigDecimal.valueOf(65),
                com.interntrack.api.entity.enums.PreferredWorkMode.HYBRID, java.time.Instant.now(), java.time.Instant.now()
        );

        when(userService.updatePreferences(eq(101L), any())).thenReturn(resp);

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targetTitle").value("SWE Intern"))
                .andExpect(jsonPath("$.data.targetLocation").value("Seattle, WA"))
                .andExpect(jsonPath("$.data.preferredWorkMode").value("HYBRID"))
                .andExpect(jsonPath("$.message").value("User profile and preferences updated successfully"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me with invalid salary range returns 400 Bad Request")
    void updateProfileAndPreferences_invalidSalary_returns400() throws Exception {
        com.interntrack.api.dto.user.UpdateUserProfilePreferencesRequest request = new com.interntrack.api.dto.user.UpdateUserProfilePreferencesRequest(
                null, "SWE", "City", java.math.BigDecimal.valueOf(100), java.math.BigDecimal.valueOf(50), com.interntrack.api.entity.enums.PreferredWorkMode.ANY
        );

        when(userService.updatePreferences(eq(101L), any()))
                .thenThrow(new IllegalArgumentException("Target minimum salary cannot exceed target maximum salary"));

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Target minimum salary cannot exceed target maximum salary"));
    }
}