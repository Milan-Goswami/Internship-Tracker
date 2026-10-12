package com.interntrack.api.controller;

import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.Interview;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.InterviewOutcome;
import com.interntrack.api.entity.enums.InterviewRoundType;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.JwtService;
import com.interntrack.api.service.InterviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.validation.method.MethodValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import com.interntrack.api.exception.GlobalExceptionHandler;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=TEST_INTERVIEW_CONTROLLER_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class InterviewControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private InterviewService interviewService;

    private MockMvc mockMvc;
    private String userToken;
    private final Long USER_ID = 100L;

    private Interview sampleInterview;
    private Application sampleApplication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        AuthenticatedUser authUser = new AuthenticatedUser(USER_ID, "applicant@example.com", "Applicant", UserRole.ROLE_USER, true);
        userToken = "Bearer " + jwtService.generateAccessToken(authUser);

        User user = new User("applicant@example.com", "hash", "Applicant", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(user, "id", USER_ID);

        sampleApplication = new Application();
        ReflectionTestUtils.setField(sampleApplication, "id", 10L);
        sampleApplication.setUser(user);

        sampleInterview = new Interview();
        ReflectionTestUtils.setField(sampleInterview, "id", 50L);
        sampleInterview.setApplication(sampleApplication);
        sampleInterview.setRoundNumber(1);
        sampleInterview.setRoundTitle("Technical Screen");
        sampleInterview.setRoundType(InterviewRoundType.TECHNICAL_OA);
        sampleInterview.setScheduledAt(Instant.parse("2026-10-01T14:00:00Z"));
        sampleInterview.setDurationMinutes(60);
        sampleInterview.setOutcome(InterviewOutcome.PENDING);
        ReflectionTestUtils.setField(sampleInterview, "createdAt", Instant.parse("2026-09-01T10:00:00Z"));
        ReflectionTestUtils.setField(sampleInterview, "updatedAt", Instant.parse("2026-09-01T10:00:00Z"));
    }

    // 1. listInterviews
    @Test
    @DisplayName("GET /api/v1/applications/{appId}/interviews without token returns 401")
    void listInterviews_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/applications/10/interviews"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/applications/{appId}/interviews returns 200 with list of rounds")
    void listInterviews_success_returns200WithList() throws Exception {
        when(interviewService.getInterviewsForApplication(10L, USER_ID))
                .thenReturn(List.of(sampleInterview));

        mockMvc.perform(get("/api/v1/applications/10/interviews")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(50))
                .andExpect(jsonPath("$.data[0].roundTitle").value("Technical Screen"))
                .andExpect(jsonPath("$.data[0].outcome").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/applications/{appId}/interviews for other user's app returns 403")
    void listInterviews_otherUserApp_returns403() throws Exception {
        when(interviewService.getInterviewsForApplication(10L, USER_ID))
                .thenThrow(new AccessDeniedException("Unauthorized: User does not own the associated application"));

        mockMvc.perform(get("/api/v1/applications/10/interviews")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/applications/{appId}/interviews for non-existent app returns 404")
    void listInterviews_appNotFound_returns404() throws Exception {
        when(interviewService.getInterviewsForApplication(999L, USER_ID))
                .thenThrow(new ResourceNotFoundException("Application not found with id: 999"));

        mockMvc.perform(get("/api/v1/applications/999/interviews")
                        .header("Authorization", userToken))
                .andExpect(status().isNotFound());
    }

    // 2. createInterview
    @Test
    @DisplayName("POST /api/v1/applications/{appId}/interviews without token returns 401")
    void createInterview_unauthenticated_returns401() throws Exception {
        String req = """
                {
                    "roundNumber": 1,
                    "roundTitle": "Screen",
                    "roundType": "RECRUITER_SCREEN",
                    "scheduledAt": "2026-10-01T14:00:00Z",
                    "durationMinutes": 30
                }
                """;

        mockMvc.perform(post("/api/v1/applications/10/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/applications/{appId}/interviews creates interview, returns 201 with Location header")
    void createInterview_success_returns201WithLocationHeader() throws Exception {
        String req = """
                {
                    "roundNumber": 1,
                    "roundTitle": "Technical Screen",
                    "roundType": "TECHNICAL_OA",
                    "scheduledAt": "2026-10-01T14:00:00Z",
                    "durationMinutes": 60,
                    "interviewerNames": "Alex",
                    "interviewerEmails": "alex@stripe.com",
                    "prepNotes": "prep"
                }
                """;

        when(interviewService.createInterview(
                eq(10L), eq(USER_ID), eq(1), eq("Technical Screen"), eq(InterviewRoundType.TECHNICAL_OA),
                any(Instant.class), eq(60), eq("Alex"), eq("alex@stripe.com"), eq("prep")
        )).thenReturn(sampleInterview);

        mockMvc.perform(post("/api/v1/applications/10/interviews")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/interviews/50"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(50))
                .andExpect(jsonPath("$.data.roundTitle").value("Technical Screen"));
    }

    @Test
    @DisplayName("POST /api/v1/applications/{appId}/interviews fails validation on blank title or non-positive round")
    void createInterview_validationFailure_returns400() throws Exception {
        String invalidReq = """
                {
                    "roundNumber": 0,
                    "roundTitle": "   ",
                    "roundType": null,
                    "scheduledAt": null,
                    "durationMinutes": -10
                }
                """;

        mockMvc.perform(post("/api/v1/applications/10/interviews")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidReq))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value("Invalid request parameters"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.details.roundNumber").exists())
                .andExpect(jsonPath("$.details.roundTitle").exists())
                .andExpect(jsonPath("$.details.roundType").exists())
                .andExpect(jsonPath("$.details.scheduledAt").exists())
                .andExpect(jsonPath("$.details.durationMinutes").exists());

        verify(interviewService, never()).createInterview(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GlobalExceptionHandler handles HandlerMethodValidationException and returns 400 with standard envelope")
    void handlerMethodValidationException_returns400WithStandardEnvelope() {
        MethodValidationResult mvr = mock(MethodValidationResult.class);
        ParameterValidationResult pvr = mock(ParameterValidationResult.class);
        FieldError fe1 = new FieldError("createInterviewRequest", "roundNumber", "Round number must be a positive integer");
        FieldError fe2 = new FieldError("createInterviewRequest", "roundTitle", "Round title is required");

        when(pvr.getResolvableErrors()).thenReturn(List.of(fe1, fe2));
        when(mvr.getParameterValidationResults()).thenReturn(List.of(pvr));

        HandlerMethodValidationException ex = new HandlerMethodValidationException(mvr);

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.http.ResponseEntity<Map<String, Object>> response = handler.handleHandlerMethodValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(false);
        assertThat(response.getBody().get("error")).isEqualTo("Validation Failed");
        assertThat(response.getBody().get("message")).isEqualTo("Invalid request parameters");

        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) response.getBody().get("details");
        assertThat(details).containsEntry("roundNumber", "Round number must be a positive integer");
        assertThat(details).containsEntry("roundTitle", "Round title is required");
    }

    @Test
    @DisplayName("POST /api/v1/applications/{appId}/interviews for other user's app returns 403")
    void createInterview_otherUserApp_returns403() throws Exception {
        String req = """
                {
                    "roundNumber": 1,
                    "roundTitle": "Screen",
                    "roundType": "RECRUITER_SCREEN",
                    "scheduledAt": "2026-10-01T14:00:00Z",
                    "durationMinutes": 30
                }
                """;

        when(interviewService.createInterview(eq(10L), eq(USER_ID), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new AccessDeniedException("Unauthorized: User does not own the associated application"));

        mockMvc.perform(post("/api/v1/applications/10/interviews")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/applications/{appId}/interviews for non-existent app returns 404")
    void createInterview_appNotFound_returns404() throws Exception {
        String req = """
                {
                    "roundNumber": 1,
                    "roundTitle": "Screen",
                    "roundType": "RECRUITER_SCREEN",
                    "scheduledAt": "2026-10-01T14:00:00Z",
                    "durationMinutes": 30
                }
                """;

        when(interviewService.createInterview(eq(999L), eq(USER_ID), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Application not found with id: 999"));

        mockMvc.perform(post("/api/v1/applications/999/interviews")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/applications/{appId}/interviews ignores rogue mass-assignment fields")
    void createInterview_massAssignmentIgnored() throws Exception {
        String payloadWithRogueFields = """
                {
                    "id": 9999,
                    "applicationId": 8888,
                    "roundNumber": 1,
                    "roundTitle": "Technical Screen",
                    "roundType": "TECHNICAL_OA",
                    "scheduledAt": "2026-10-01T14:00:00Z",
                    "durationMinutes": 60,
                    "outcome": "PASSED",
                    "user": { "id": 1 },
                    "userId": 999
                }
                """;

        when(interviewService.createInterview(
                eq(10L), eq(USER_ID), eq(1), eq("Technical Screen"), eq(InterviewRoundType.TECHNICAL_OA),
                any(Instant.class), eq(60), isNull(), isNull(), isNull()
        )).thenReturn(sampleInterview);

        mockMvc.perform(post("/api/v1/applications/10/interviews")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithRogueFields))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(50));
    }

    // 3. getInterview
    @Test
    @DisplayName("GET /api/v1/interviews/{interviewId} without token returns 401")
    void getInterview_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/interviews/50"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/interviews/{interviewId} returns 200 with interview details")
    void getInterview_success_returns200() throws Exception {
        when(interviewService.getInterviewById(50L, USER_ID)).thenReturn(sampleInterview);

        mockMvc.perform(get("/api/v1/interviews/50")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(50))
                .andExpect(jsonPath("$.data.applicationId").value(10))
                .andExpect(jsonPath("$.data.roundTitle").value("Technical Screen"));
    }

    @Test
    @DisplayName("GET /api/v1/interviews/{interviewId} for missing round returns 404")
    void getInterview_notFound_returns404() throws Exception {
        when(interviewService.getInterviewById(999L, USER_ID))
                .thenThrow(new ResourceNotFoundException("Interview not found with id: 999"));

        mockMvc.perform(get("/api/v1/interviews/999")
                        .header("Authorization", userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/interviews/{interviewId} for other user's round returns 403")
    void getInterview_otherUser_returns403() throws Exception {
        when(interviewService.getInterviewById(50L, USER_ID))
                .thenThrow(new AccessDeniedException("Unauthorized: User does not own the associated application"));

        mockMvc.perform(get("/api/v1/interviews/50")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    // 4. updateInterview
    @Test
    @DisplayName("PUT /api/v1/interviews/{interviewId} without token returns 401")
    void updateInterview_unauthenticated_returns401() throws Exception {
        String req = """
                {
                    "roundNumber": 2,
                    "roundTitle": "Updated"
                }
                """;

        mockMvc.perform(put("/api/v1/interviews/50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/interviews/{interviewId} returns 200 with updated interview")
    void updateInterview_success_returns200() throws Exception {
        String req = """
                {
                    "roundNumber": 2,
                    "roundTitle": "Updated Screen",
                    "roundType": "LIVE_CODING",
                    "scheduledAt": "2026-10-02T15:00:00Z",
                    "durationMinutes": 90,
                    "interviewerNames": "Interviewer 2",
                    "interviewerEmails": "i2@stripe.com",
                    "prepNotes": "prep 2",
                    "reflectionNotes": "refl 2",
                    "outcome": "PASSED"
                }
                """;

        sampleInterview.setRoundTitle("Updated Screen");
        sampleInterview.setOutcome(InterviewOutcome.PASSED);

        when(interviewService.updateInterview(
                eq(50L), eq(USER_ID), eq(2), eq("Updated Screen"), eq(InterviewRoundType.LIVE_CODING),
                any(Instant.class), eq(90), eq("Interviewer 2"), eq("i2@stripe.com"),
                eq("prep 2"), eq("refl 2"), eq(InterviewOutcome.PASSED)
        )).thenReturn(sampleInterview);

        mockMvc.perform(put("/api/v1/interviews/50")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roundTitle").value("Updated Screen"))
                .andExpect(jsonPath("$.data.outcome").value("PASSED"));
    }

    @Test
    @DisplayName("PUT /api/v1/interviews/{interviewId} with invalid round number returns 400")
    void updateInterview_validationFailure_returns400() throws Exception {
        String invalidReq = """
                {
                    "roundNumber": -1
                }
                """;

        mockMvc.perform(put("/api/v1/interviews/50")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidReq))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/interviews/{interviewId} for other user returns 403")
    void updateInterview_otherUser_returns403() throws Exception {
        String req = """
                {
                    "roundNumber": 2,
                    "roundTitle": "Updated"
                }
                """;

        when(interviewService.updateInterview(eq(50L), eq(USER_ID), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new AccessDeniedException("Unauthorized: User does not own the associated application"));

        mockMvc.perform(put("/api/v1/interviews/50")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/interviews/{interviewId} for missing round returns 404")
    void updateInterview_notFound_returns404() throws Exception {
        String req = """
                {
                    "roundNumber": 2,
                    "roundTitle": "Updated"
                }
                """;

        when(interviewService.updateInterview(eq(999L), eq(USER_ID), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Interview not found with id: 999"));

        mockMvc.perform(put("/api/v1/interviews/999")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isNotFound());
    }

    // 5. recordOutcome
    @Test
    @DisplayName("PATCH /api/v1/interviews/{interviewId}/outcome without token returns 401")
    void recordOutcome_unauthenticated_returns401() throws Exception {
        String req = """
                {
                    "outcome": "PASSED",
                    "reflectionNotes": "Great round"
                }
                """;

        mockMvc.perform(patch("/api/v1/interviews/50/outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/v1/interviews/{interviewId}/outcome records outcome and returns 200")
    void recordOutcome_success_returns200() throws Exception {
        String req = """
                {
                    "outcome": "PASSED",
                    "reflectionNotes": "Great round"
                }
                """;

        sampleInterview.setOutcome(InterviewOutcome.PASSED);
        sampleInterview.setReflectionNotes("Great round");

        when(interviewService.recordOutcome(50L, USER_ID, InterviewOutcome.PASSED, "Great round"))
                .thenReturn(sampleInterview);

        mockMvc.perform(patch("/api/v1/interviews/50/outcome")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.outcome").value("PASSED"))
                .andExpect(jsonPath("$.data.reflectionNotes").value("Great round"));
    }

    @Test
    @DisplayName("PATCH /api/v1/interviews/{interviewId}/outcome with missing outcome returns 400")
    void recordOutcome_missingOutcome_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/interviews/50/outcome")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\": null}"))
                .andExpect(status().isBadRequest());
    }

    // 6. deleteInterview
    @Test
    @DisplayName("DELETE /api/v1/interviews/{interviewId} without token returns 401")
    void deleteInterview_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/interviews/50"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/v1/interviews/{interviewId} deletes round and returns 204")
    void deleteInterview_success_returns204() throws Exception {
        doNothing().when(interviewService).deleteInterview(50L, USER_ID);

        mockMvc.perform(delete("/api/v1/interviews/50")
                        .header("Authorization", userToken))
                .andExpect(status().isNoContent());

        verify(interviewService).deleteInterview(50L, USER_ID);
    }

    @Test
    @DisplayName("DELETE /api/v1/interviews/{interviewId} for other user returns 403")
    void deleteInterview_otherUser_returns403() throws Exception {
        doThrow(new AccessDeniedException("Unauthorized: User does not own the associated application"))
                .when(interviewService).deleteInterview(50L, USER_ID);

        mockMvc.perform(delete("/api/v1/interviews/50")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/interviews/{interviewId} for non-existent round returns 404")
    void deleteInterview_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Interview not found with id: 999"))
                .when(interviewService).deleteInterview(999L, USER_ID);

        mockMvc.perform(delete("/api/v1/interviews/999")
                        .header("Authorization", userToken))
                .andExpect(status().isNotFound());
    }
}
