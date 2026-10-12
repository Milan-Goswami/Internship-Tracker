package com.interntrack.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interntrack.api.dto.user.UpdateUserProfilePreferencesRequest;
import com.interntrack.api.dto.user.UpdateUserProfileRequest;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.UserProfile;
import com.interntrack.api.entity.enums.PreferredWorkMode;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.repository.UserProfileRepository;
import com.interntrack.api.repository.UserRepository;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=USER_PROFILE_INTEGRATION_TEST_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class UserProfileIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private String userToken;
    private User testUser;

    private static final String TEST_EMAIL = "profile_integration_user@example.com";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        cleanDatabase();

        testUser = new User(TEST_EMAIL, "pwd123456", "Profile Candidate", UserRole.ROLE_USER);
        testUser = userRepository.save(testUser);

        AuthenticatedUser principal = new AuthenticatedUser(
                testUser.getId(),
                testUser.getEmail(),
                testUser.getFullName(),
                testUser.getRole(),
                true
        );
        userToken = "Bearer " + jwtService.generateAccessToken(principal);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        userRepository.findByEmail(TEST_EMAIL).ifPresent(u -> {
            userProfileRepository.findById(u.getId()).ifPresent(userProfileRepository::delete);
            userProfileRepository.flush();

            userRepository.delete(u);
            userRepository.flush();
        });
    }

    @Test
    @DisplayName("GET /api/v1/users/me returns 200 with default preferences when user has no user_profiles row")
    void firstUse_whenNoProfileRowExists_returnsDefaultPreferences() throws Exception {
        assertThat(userProfileRepository.findById(testUser.getId())).isEmpty();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testUser.getId()))
                .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.data.fullName").value("Profile Candidate"))
                .andExpect(jsonPath("$.data.targetTitle").doesNotExist())
                .andExpect(jsonPath("$.data.preferredWorkMode").value("ANY"))
                .andExpect(jsonPath("$.message").value("User profile retrieved successfully"));
    }

    @Test
    @DisplayName("Complete PUT and GET lifecycle: creates UserProfile row, persists preferences, and updates existing row")
    void profilePreferences_completeLifecycle() throws Exception {
        // 1. Initial state: no profile row
        assertThat(userProfileRepository.findById(testUser.getId())).isEmpty();

        // 2. PUT /api/v1/users/me creates UserProfile row
        UpdateUserProfilePreferencesRequest createReq = new UpdateUserProfilePreferencesRequest(
                "Updated Candidate", "Software Engineer Intern", "Seattle, WA",
                BigDecimal.valueOf(45.00), BigDecimal.valueOf(65.00), PreferredWorkMode.HYBRID
        );

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Updated Candidate"))
                .andExpect(jsonPath("$.data.targetTitle").value("Software Engineer Intern"))
                .andExpect(jsonPath("$.data.targetLocation").value("Seattle, WA"))
                .andExpect(jsonPath("$.data.targetSalaryMin").value(45.00))
                .andExpect(jsonPath("$.data.targetSalaryMax").value(65.00))
                .andExpect(jsonPath("$.data.preferredWorkMode").value("HYBRID"));

        // Verify row was physically created in user_profiles
        Optional<UserProfile> createdProfile = userProfileRepository.findById(testUser.getId());
        assertThat(createdProfile).isPresent();
        assertThat(createdProfile.get().getTargetTitle()).isEqualTo("Software Engineer Intern");
        assertThat(createdProfile.get().getPreferredWorkMode()).isEqualTo(PreferredWorkMode.HYBRID);

        // 3. GET /api/v1/users/me returns persisted preferences
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetTitle").value("Software Engineer Intern"))
                .andExpect(jsonPath("$.data.preferredWorkMode").value("HYBRID"));

        // 4. PUT /api/v1/users/me updates existing row
        UpdateUserProfilePreferencesRequest updateReq = new UpdateUserProfilePreferencesRequest(
                "Updated Candidate", "Senior Intern", "Remote, US",
                BigDecimal.valueOf(55.00), BigDecimal.valueOf(80.00), PreferredWorkMode.REMOTE
        );

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetTitle").value("Senior Intern"))
                .andExpect(jsonPath("$.data.preferredWorkMode").value("REMOTE"));

        // Verify still exactly 1 row
        Optional<UserProfile> updatedProfile = userProfileRepository.findById(testUser.getId());
        assertThat(updatedProfile).isPresent();
        assertThat(updatedProfile.get().getTargetTitle()).isEqualTo("Senior Intern");
        assertThat(updatedProfile.get().getPreferredWorkMode()).isEqualTo(PreferredWorkMode.REMOTE);

        // 5. PATCH /api/v1/users/me updates full name and preserves existing preferences
        UpdateUserProfileRequest patchReq = new UpdateUserProfileRequest("Alice Final Name");
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Alice Final Name"))
                .andExpect(jsonPath("$.data.targetTitle").value("Senior Intern"))
                .andExpect(jsonPath("$.data.preferredWorkMode").value("REMOTE"));

        // 6. PUT with invalid salary range returns 400
        UpdateUserProfilePreferencesRequest invalidReq = new UpdateUserProfilePreferencesRequest(
                null, "SWE", "City", BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00), PreferredWorkMode.ANY
        );

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}