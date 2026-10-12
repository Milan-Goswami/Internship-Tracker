package com.interntrack.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interntrack.api.dto.application.BatchUpdateStatusRequest;
import com.interntrack.api.dto.application.CreateApplicationRequest;
import com.interntrack.api.dto.application.UpdateApplicationRequest;
import com.interntrack.api.dto.application.UpdateApplicationStatusRequest;
import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.Company;
import com.interntrack.api.entity.Opportunity;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.ApplicationStatus;
import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.SourceProvider;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.DuplicateApplicationException;
import com.interntrack.api.exception.InvalidStatusTransitionException;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.JwtService;
import com.interntrack.api.service.ApplicationService;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=TEST_APPLICATION_CONTROLLER_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class ApplicationControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ApplicationService applicationService;

    private MockMvc mockMvc;
    private String userToken;
    private Application sampleApplication;
    private User sampleUser;
    private Opportunity sampleOpportunity;
    private Company sampleCompany;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userToken = "Bearer " + jwtService.generateAccessToken(
                new AuthenticatedUser(10L, "candidate@example.com", "Candidate", UserRole.ROLE_USER, true)
        );

        sampleUser = new User("candidate@example.com", "hash", "Candidate", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(sampleUser, "id", 10L);

        sampleCompany = new Company("Stripe", "stripe.com", "Fintech");
        ReflectionTestUtils.setField(sampleCompany, "id", 100L);
        sampleCompany.setLogoUrl("https://example.com/logo.png");

        sampleOpportunity = new Opportunity(sampleCompany, "Backend Intern", LocationType.REMOTE, SourceProvider.SYSTEM);
        ReflectionTestUtils.setField(sampleOpportunity, "id", 50L);

        sampleApplication = new Application(sampleUser, sampleOpportunity, ApplicationStatus.SAVED);
        ReflectionTestUtils.setField(sampleApplication, "id", 1L);
        sampleApplication.setPersonalNotes("Applied via referral");
        sampleApplication.setUserRating(4);
    }

    @Test
    @DisplayName("GET /api/v1/applications without token returns 401 Unauthorized")
    void listApplications_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/applications returns 200 with user applications list")
    void listApplications_authenticated_returns200() throws Exception {
        when(applicationService.getApplicationsForUser(10L, null))
                .thenReturn(List.of(sampleApplication));

        mockMvc.perform(get("/api/v1/applications")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].opportunityId").value(50))
                .andExpect(jsonPath("$.data[0].opportunityTitle").value("Backend Intern"))
                .andExpect(jsonPath("$.data[0].companyName").value("Stripe"))
                .andExpect(jsonPath("$.data[0].status").value("SAVED"));
    }

    @Test
    @DisplayName("GET /api/v1/applications with status filter passes filter to service")
    void listApplications_withStatus_returns200() throws Exception {
        when(applicationService.getApplicationsForUser(10L, ApplicationStatus.SAVED))
                .thenReturn(List.of(sampleApplication));

        mockMvc.perform(get("/api/v1/applications?status=SAVED")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(applicationService).getApplicationsForUser(10L, ApplicationStatus.SAVED);
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id} without token returns 401 Unauthorized")
    void getApplicationById_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/applications/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id} for owned application returns 200 OK")
    void getApplicationById_whenOwner_returns200() throws Exception {
        when(applicationService.getApplicationById(1L, 10L)).thenReturn(sampleApplication);

        mockMvc.perform(get("/api/v1/applications/1")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.opportunityId").value(50))
                .andExpect(jsonPath("$.data.status").value("SAVED"));
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id} for another user application returns 403 Forbidden per SECURITY_PLAN 2.2")
    void getApplicationById_whenAnotherUser_returns403() throws Exception {
        when(applicationService.getApplicationById(2L, 10L))
                .thenThrow(new AccessDeniedException("Unauthorized access to requested application"));

        mockMvc.perform(get("/api/v1/applications/2")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id} for non-existent application returns 404 Not Found")
    void getApplicationById_whenNotFound_returns404() throws Exception {
        when(applicationService.getApplicationById(999L, 10L))
                .thenThrow(new ResourceNotFoundException("Application not found with id: 999"));

        mockMvc.perform(get("/api/v1/applications/999")
                        .header("Authorization", userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("POST /api/v1/applications without token returns 401 Unauthorized")
    void createApplication_withoutToken_returns401() throws Exception {
        CreateApplicationRequest req = new CreateApplicationRequest(50L, ApplicationStatus.SAVED, null, null, null, null);
        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/applications with valid body returns 201 Created and Location header")
    void createApplication_valid_returns201() throws Exception {
        when(applicationService.createApplication(
                eq(10L), eq(50L), eq(ApplicationStatus.SAVED), isNull(), isNull(), eq("Notes"), eq(3)
        )).thenReturn(sampleApplication);

        CreateApplicationRequest req = new CreateApplicationRequest(
                50L, ApplicationStatus.SAVED, null, null, "Notes", 3
        );

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/applications/1"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/applications with missing opportunityId returns 400 Bad Request")
    void createApplication_missingOpportunityId_returns400() throws Exception {
        String json = "{\"status\":\"SAVED\"}";

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/v1/applications with invalid rating returns 400 Bad Request")
    void createApplication_invalidRating_returns400() throws Exception {
        CreateApplicationRequest req = new CreateApplicationRequest(
                50L, ApplicationStatus.SAVED, null, null, null, 6
        );

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/v1/applications when opportunity not found returns 404 Not Found")
    void createApplication_opportunityNotFound_returns404() throws Exception {
        when(applicationService.createApplication(eq(10L), eq(999L), any(), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Opportunity not found with id: 999"));

        CreateApplicationRequest req = new CreateApplicationRequest(999L, ApplicationStatus.SAVED, null, null, null, null);

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("POST /api/v1/applications when duplicate opportunity tracked returns 409 Conflict")
    void createApplication_duplicate_returns409() throws Exception {
        when(applicationService.createApplication(eq(10L), eq(50L), any(), any(), any(), any(), any()))
                .thenThrow(new DuplicateApplicationException("Application already exists for user ID 10 and opportunity ID 50"));

        CreateApplicationRequest req = new CreateApplicationRequest(50L, ApplicationStatus.SAVED, null, null, null, null);

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Application already exists for user ID 10 and opportunity ID 50"));
    }

    @Test
    @DisplayName("POST /api/v1/applications enforces mass assignment defense ignoring rogue client fields")
    void createApplication_massAssignmentDefense_usesJwtPrincipalOnly() throws Exception {
        when(applicationService.createApplication(
                eq(10L), eq(50L), eq(ApplicationStatus.SAVED), isNull(), isNull(), isNull(), isNull()
        )).thenReturn(sampleApplication);

        String maliciousJson = "{\"opportunityId\":50,\"userId\":9999,\"id\":8888,\"savedAt\":\"1990-01-01T00:00:00Z\",\"status\":\"SAVED\"}";

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousJson))
                .andExpect(status().isCreated());

        verify(applicationService).createApplication(
                eq(10L), eq(50L), eq(ApplicationStatus.SAVED), isNull(), isNull(), isNull(), isNull()
        );
    }

    @Test
    @DisplayName("PATCH /api/v1/applications/{id}/status without token returns 401 Unauthorized")
    void updateStatus_withoutToken_returns401() throws Exception {
        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest(ApplicationStatus.APPLIED);
        mockMvc.perform(patch("/api/v1/applications/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/v1/applications/{id}/status valid transition returns 200 OK")
    void updateStatus_validTransition_returns200() throws Exception {
        sampleApplication.setStatus(ApplicationStatus.APPLIED);
        when(applicationService.transitionStatus(1L, 10L, ApplicationStatus.APPLIED))
                .thenReturn(sampleApplication);

        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest(ApplicationStatus.APPLIED);

        mockMvc.perform(patch("/api/v1/applications/1/status")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPLIED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/applications/{id}/status invalid transition returns 400 Bad Request")
    void updateStatus_invalidTransition_returns400() throws Exception {
        when(applicationService.transitionStatus(1L, 10L, ApplicationStatus.OFFER))
                .thenThrow(new InvalidStatusTransitionException("Invalid status transition from SAVED to OFFER"));

        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest(ApplicationStatus.OFFER);

        mockMvc.perform(patch("/api/v1/applications/1/status")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid status transition from SAVED to OFFER"));
    }

    @Test
    @DisplayName("PATCH /api/v1/applications/{id}/status on another user application returns 403 Forbidden")
    void updateStatus_anotherUser_returns403() throws Exception {
        when(applicationService.transitionStatus(2L, 10L, ApplicationStatus.APPLIED))
                .thenThrow(new AccessDeniedException("Unauthorized: User does not own this application"));

        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest(ApplicationStatus.APPLIED);

        mockMvc.perform(patch("/api/v1/applications/2/status")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("PATCH /api/v1/applications/batch-status valid batch returns 200 OK")
    void batchUpdateStatus_valid_returns200() throws Exception {
        sampleApplication.setStatus(ApplicationStatus.APPLIED);
        when(applicationService.batchTransitionStatus(List.of(1L), 10L, ApplicationStatus.APPLIED))
                .thenReturn(List.of(sampleApplication));

        BatchUpdateStatusRequest req = new BatchUpdateStatusRequest(List.of(1L), ApplicationStatus.APPLIED);

        mockMvc.perform(patch("/api/v1/applications/batch-status")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status").value("APPLIED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/applications/batch-status with mixed-ownership batch returns 403 Forbidden and modifies zero records")
    void batchUpdateStatus_mixedOwnershipBatch_returns403() throws Exception {
        when(applicationService.batchTransitionStatus(List.of(1L, 2L), 10L, ApplicationStatus.APPLIED))
                .thenThrow(new AccessDeniedException("Unauthorized: User does not own application with id: 2"));

        BatchUpdateStatusRequest req = new BatchUpdateStatusRequest(List.of(1L, 2L), ApplicationStatus.APPLIED);

        mockMvc.perform(patch("/api/v1/applications/batch-status")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("PATCH /api/v1/applications/batch-status with empty applicationIds returns 400 Bad Request")
    void batchUpdateStatus_emptyList_returns400() throws Exception {
        String json = "{\"applicationIds\":[],\"status\":\"APPLIED\"}";

        mockMvc.perform(patch("/api/v1/applications/batch-status")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("PUT /api/v1/applications/{id} valid notes and rating update returns 200 OK")
    void updateApplication_valid_returns200() throws Exception {
        sampleApplication.setPersonalNotes("Updated notes");
        sampleApplication.setUserRating(5);

        when(applicationService.updateNotesAndRating(eq(1L), eq(10L), eq("Updated notes"), eq(5), isNull(), isNull()))
                .thenReturn(sampleApplication);

        UpdateApplicationRequest req = new UpdateApplicationRequest("Updated notes", 5, null, null);

        mockMvc.perform(put("/api/v1/applications/1")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.personalNotes").value("Updated notes"))
                .andExpect(jsonPath("$.data.userRating").value(5));
    }

    @Test
    @DisplayName("PUT /api/v1/applications/{id} on another user application returns 403 Forbidden")
    void updateApplication_anotherUser_returns403() throws Exception {
        when(applicationService.updateNotesAndRating(eq(2L), eq(10L), any(), any(), any(), any()))
                .thenThrow(new AccessDeniedException("Unauthorized: User does not own this application"));

        UpdateApplicationRequest req = new UpdateApplicationRequest("Hacker notes", 5, null, null);

        mockMvc.perform(put("/api/v1/applications/2")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("DELETE /api/v1/applications/{id} without token returns 401 Unauthorized")
    void deleteApplication_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/applications/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/v1/applications/{id} by owner returns 204 No Content")
    void deleteApplication_byOwner_returns204() throws Exception {
        doNothing().when(applicationService).deleteApplication(1L, 10L);

        mockMvc.perform(delete("/api/v1/applications/1")
                        .header("Authorization", userToken))
                .andExpect(status().isNoContent());

        verify(applicationService).deleteApplication(1L, 10L);
    }

    @Test
    @DisplayName("DELETE /api/v1/applications/{id} on another user application returns 403 Forbidden")
    void deleteApplication_anotherUser_returns403() throws Exception {
        doThrow(new AccessDeniedException("Unauthorized: User does not own this application"))
                .when(applicationService).deleteApplication(2L, 10L);

        mockMvc.perform(delete("/api/v1/applications/2")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("DELETE /api/v1/applications/{id} on non-existent application returns 404 Not Found")
    void deleteApplication_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Application not found with id: 999"))
                .when(applicationService).deleteApplication(999L, 10L);

        mockMvc.perform(delete("/api/v1/applications/999")
                        .header("Authorization", userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}