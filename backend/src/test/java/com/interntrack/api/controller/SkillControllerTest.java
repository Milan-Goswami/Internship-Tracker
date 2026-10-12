package com.interntrack.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interntrack.api.dto.skill.AddUserSkillRequest;
import com.interntrack.api.dto.skill.SkillResponse;
import com.interntrack.api.dto.skill.UserSkillResponse;
import com.interntrack.api.entity.enums.ProficiencyLevel;
import com.interntrack.api.entity.enums.SkillCategory;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.DuplicateSkillException;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.JwtService;
import com.interntrack.api.service.SkillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=TEST_SKILL_CONTROLLER_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class SkillControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SkillService skillService;

    private MockMvc mockMvc;
    private String userToken;
    private final Long USER_ID = 300L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        AuthenticatedUser authUser = new AuthenticatedUser(USER_ID, "cand@example.com", "Cand", UserRole.ROLE_USER, true);
        userToken = "Bearer " + jwtService.generateAccessToken(authUser);
    }

    @Test
    @DisplayName("GET /api/v1/skills/search without token returns 401 Unauthorized")
    void searchSkills_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/skills/search"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/skills/search with token returns 200 OK")
    void searchSkills_withToken_returns200() throws Exception {
        SkillResponse resp = new SkillResponse(1L, "Java", "java", SkillCategory.LANGUAGE);
        when(skillService.searchSkills("jav")).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/v1/skills/search?q=jav")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Java"))
                .andExpect(jsonPath("$.data[0].canonicalName").value("java"));
    }

    @Test
    @DisplayName("GET /api/v1/skills/user without token returns 401 Unauthorized")
    void getUserSkills_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/skills/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/skills/user with token returns 200 OK")
    void getUserSkills_withToken_returns200() throws Exception {
        UserSkillResponse resp = new UserSkillResponse(1L, "Java", "java", SkillCategory.LANGUAGE, ProficiencyLevel.ADVANCED, 3);
        when(skillService.getUserSkills(USER_ID)).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/v1/skills/user")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].skillName").value("Java"))
                .andExpect(jsonPath("$.data[0].proficiencyLevel").value("ADVANCED"));
    }

    @Test
    @DisplayName("POST /api/v1/skills/user without token returns 401 Unauthorized")
    void addUserSkill_withoutToken_returns401() throws Exception {
        AddUserSkillRequest req = new AddUserSkillRequest(1L, ProficiencyLevel.INTERMEDIATE, 2);
        mockMvc.perform(post("/api/v1/skills/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/skills/user with valid payload returns 201 Created")
    void addUserSkill_valid_returns201() throws Exception {
        AddUserSkillRequest req = new AddUserSkillRequest(1L, ProficiencyLevel.ADVANCED, 3);
        UserSkillResponse resp = new UserSkillResponse(1L, "Java", "java", SkillCategory.LANGUAGE, ProficiencyLevel.ADVANCED, 3);

        when(skillService.addUserSkill(eq(USER_ID), eq(req))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/skills/user")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.skillName").value("Java"))
                .andExpect(jsonPath("$.data.proficiencyLevel").value("ADVANCED"))
                .andExpect(jsonPath("$.message").value("Skill added to user profile successfully"));
    }

    @Test
    @DisplayName("POST /api/v1/skills/user with duplicate skill returns 409 Conflict")
    void addUserSkill_duplicate_returns409() throws Exception {
        AddUserSkillRequest req = new AddUserSkillRequest(1L, ProficiencyLevel.ADVANCED, 3);

        when(skillService.addUserSkill(eq(USER_ID), any()))
                .thenThrow(new DuplicateSkillException("Skill is already added to user profile"));

        mockMvc.perform(post("/api/v1/skills/user")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Skill is already added to user profile"));
    }

    @Test
    @DisplayName("POST /api/v1/skills/user with missing skillId returns 400 Bad Request")
    void addUserSkill_missingSkillId_returns400() throws Exception {
        String payload = "{\"proficiencyLevel\":\"INTERMEDIATE\",\"yearsOfExperience\":2}";

        mockMvc.perform(post("/api/v1/skills/user")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/v1/skills/user with invalid experience (-1) returns 400 Bad Request")
    void addUserSkill_negativeExperience_returns400() throws Exception {
        AddUserSkillRequest req = new AddUserSkillRequest(1L, ProficiencyLevel.INTERMEDIATE, -1);

        mockMvc.perform(post("/api/v1/skills/user")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /api/v1/skills/user/{skillId} without token returns 401 Unauthorized")
    void removeUserSkill_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/skills/user/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/v1/skills/user/{skillId} valid returns 204 No Content")
    void removeUserSkill_valid_returns204() throws Exception {
        doNothing().when(skillService).removeUserSkill(USER_ID, 1L);

        mockMvc.perform(delete("/api/v1/skills/user/1")
                        .header("Authorization", userToken))
                .andExpect(status().isNoContent());

        verify(skillService).removeUserSkill(USER_ID, 1L);
    }

    @Test
    @DisplayName("DELETE /api/v1/skills/user/{skillId} non-existent returns 404 Not Found")
    void removeUserSkill_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Skill not found on user profile"))
                .when(skillService).removeUserSkill(USER_ID, 999L);

        mockMvc.perform(delete("/api/v1/skills/user/999")
                        .header("Authorization", userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}