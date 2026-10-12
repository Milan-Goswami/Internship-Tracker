package com.interntrack.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interntrack.api.dto.opportunity.CreateOpportunityRequest;
import com.interntrack.api.dto.opportunity.UpdateOpportunityRequest;
import com.interntrack.api.entity.Company;
import com.interntrack.api.entity.Opportunity;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.OpportunityStatus;
import com.interntrack.api.entity.enums.SourceProvider;
import com.interntrack.api.entity.enums.StipendPeriod;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.JwtService;
import com.interntrack.api.service.OpportunityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=TEST_OPPORTUNITY_CONTROLLER_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class OpportunityControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OpportunityService opportunityService;

    @MockitoBean
    private com.interntrack.api.service.SkillService skillService;

    private MockMvc mockMvc;
    private Company sampleCompany;
    private User sampleUser;
    private Opportunity sampleOpportunity;
    private AuthenticatedUser samplePrincipal;
    private String validToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        sampleCompany = new Company("Stripe", "stripe.com", "Financial Technology");
        sampleCompany.setCanonicalName("stripe");
        ReflectionTestUtils.setField(sampleCompany, "id", 1L);

        sampleUser = new User("candidate@example.com", "hash123", "Alice Candidate", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(sampleUser, "id", 101L);

        sampleOpportunity = new Opportunity(sampleCompany, "Backend Engineering Intern", LocationType.REMOTE, SourceProvider.USER_MANUAL);
        sampleOpportunity.setCreatedByUser(sampleUser);
        sampleOpportunity.setDescription("Work on microservices");
        sampleOpportunity.setStipendAmount(new BigDecimal("45.00"));
        sampleOpportunity.setStipendCurrency("USD");
        sampleOpportunity.setStipendPeriod(StipendPeriod.HOURLY);
        sampleOpportunity.setDurationMonths(3);
        sampleOpportunity.setStatus(OpportunityStatus.ACTIVE);
        ReflectionTestUtils.setField(sampleOpportunity, "id", 42L);
        ReflectionTestUtils.setField(sampleOpportunity, "createdAt", Instant.parse("2026-09-04T12:00:00Z"));
        ReflectionTestUtils.setField(sampleOpportunity, "updatedAt", Instant.parse("2026-09-04T12:00:00Z"));

        samplePrincipal = new AuthenticatedUser(101L, "candidate@example.com", "Alice Candidate", UserRole.ROLE_USER, true);
        validToken = jwtService.generateAccessToken(samplePrincipal);
    }

    // 1. GET /api/v1/opportunities without token -> 401
    @Test
    @DisplayName("GET /api/v1/opportunities without token returns 401 Unauthorized")
    void listOpportunities_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/opportunities"))
                .andExpect(status().isUnauthorized());
    }

    // 2. GET /api/v1/opportunities with valid token -> 200
    @Test
    @DisplayName("GET /api/v1/opportunities with valid token returns 200 OK and list")
    void listOpportunities_withValidToken_returns200() throws Exception {
        when(opportunityService.findAccessibleOpportunities(eq(101L), isNull(), isNull()))
                .thenReturn(List.of(sampleOpportunity));

        mockMvc.perform(get("/api/v1/opportunities")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(42))
                .andExpect(jsonPath("$.data[0].title").value("Backend Engineering Intern"))
                .andExpect(jsonPath("$.data[0].companyName").value("Stripe"))
                .andExpect(jsonPath("$.data[0].isCustom").value(true))
                .andExpect(jsonPath("$.data[0].createdByUserId").doesNotExist()) // Never exposed
                .andExpect(jsonPath("$.message").value("Opportunities retrieved successfully"));
    }

    // 3. GET /api/v1/opportunities with query filters -> 200
    @Test
    @DisplayName("GET /api/v1/opportunities with query parameters passes filters to service")
    void listOpportunities_withFilters_passesFilters() throws Exception {
        when(opportunityService.findAccessibleOpportunities(eq(101L), eq(LocationType.REMOTE), eq("backend")))
                .thenReturn(List.of(sampleOpportunity));

        mockMvc.perform(get("/api/v1/opportunities")
                        .param("locationType", "REMOTE")
                        .param("search", "backend")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(42));

        verify(opportunityService).findAccessibleOpportunities(101L, LocationType.REMOTE, "backend");
    }

    // 4. GET /api/v1/opportunities/{id} without token -> 401
    @Test
    @DisplayName("GET /api/v1/opportunities/{id} without token returns 401 Unauthorized")
    void getOpportunity_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/opportunities/42"))
                .andExpect(status().isUnauthorized());
    }

    // 5. GET /api/v1/opportunities/{id} with valid token -> 200
    @Test
    @DisplayName("GET /api/v1/opportunities/{id} with valid token returns 200 OK and data")
    void getOpportunity_withValidToken_returns200() throws Exception {
        when(opportunityService.findAccessibleById(42L, 101L))
                .thenReturn(Optional.of(sampleOpportunity));

        mockMvc.perform(get("/api/v1/opportunities/42")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.title").value("Backend Engineering Intern"))
                .andExpect(jsonPath("$.data.isCustom").value(true))
                .andExpect(jsonPath("$.data.createdByUserId").doesNotExist()) // Never exposed
                .andExpect(jsonPath("$.message").value("Opportunity retrieved successfully"));
    }

    // 6. GET /api/v1/opportunities/{id} non-existent or inaccessible -> 404
    @Test
    @DisplayName("GET /api/v1/opportunities/{id} for non-existent or inaccessible returns 404 Not Found")
    void getOpportunity_whenInaccessible_returns404() throws Exception {
        when(opportunityService.findAccessibleById(999L, 101L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/opportunities/999")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // 7. POST /api/v1/opportunities without token -> 401
    @Test
    @DisplayName("POST /api/v1/opportunities without token returns 401 Unauthorized")
    void createOpportunity_withoutToken_returns401() throws Exception {
        CreateOpportunityRequest request = new CreateOpportunityRequest(
                1L, "Intern", "Desc", LocationType.REMOTE, "SF",
                new BigDecimal("40.00"), "USD", StipendPeriod.HOURLY, 3, null, null
        );

        mockMvc.perform(post("/api/v1/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // 8. POST /api/v1/opportunities with valid payload -> 201
    @Test
    @DisplayName("POST /api/v1/opportunities with valid payload returns 201 Created with Location header")
    void createOpportunity_withValidPayload_returns201() throws Exception {
        CreateOpportunityRequest request = new CreateOpportunityRequest(
                1L, "Backend Engineering Intern", "Work on microservices", LocationType.REMOTE, "San Francisco",
                new BigDecimal("45.00"), "USD", StipendPeriod.HOURLY, 3, null, "https://stripe.com/job"
        );

        when(opportunityService.createOpportunity(
                eq(1L),
                eq(101L), // creator bound from JWT principal
                eq(SourceProvider.USER_MANUAL), // server-controlled
                isNull(), // externalSourceId is null for USER_MANUAL
                eq("Backend Engineering Intern"),
                eq("Work on microservices"),
                eq(LocationType.REMOTE),
                eq("San Francisco"),
                eq(new BigDecimal("45.00")),
                eq("USD"),
                eq(StipendPeriod.HOURLY),
                eq(3),
                isNull(),
                eq("https://stripe.com/job"),
                isNull()
        )).thenReturn(sampleOpportunity);

        mockMvc.perform(post("/api/v1/opportunities")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/opportunities/42"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.isCustom").value(true))
                .andExpect(jsonPath("$.data.createdByUserId").doesNotExist()) // Never exposed
                .andExpect(jsonPath("$.message").value("Opportunity created successfully"));
    }

    // 9. POST /api/v1/opportunities with blank title -> 400
    @Test
    @DisplayName("POST /api/v1/opportunities with blank title returns 400 Bad Request")
    void createOpportunity_withBlankTitle_returns400() throws Exception {
        CreateOpportunityRequest request = new CreateOpportunityRequest(
                1L, "   ", "Desc", LocationType.REMOTE, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/opportunities")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // 10. POST /api/v1/opportunities with missing companyId -> 400
    @Test
    @DisplayName("POST /api/v1/opportunities with missing companyId returns 400 Bad Request")
    void createOpportunity_withNullCompanyId_returns400() throws Exception {
        CreateOpportunityRequest request = new CreateOpportunityRequest(
                null, "Title", "Desc", LocationType.REMOTE, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/opportunities")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/v1/opportunities with invalid sourceUrl scheme returns 400 Bad Request")
    void createOpportunity_withInvalidSourceUrl_returns400() throws Exception {
        CreateOpportunityRequest request = new CreateOpportunityRequest(
                1L, "Title", "Desc", LocationType.REMOTE, null, null, null, null, null, null, "javascript:alert(1)"
        );

        mockMvc.perform(post("/api/v1/opportunities")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("PUT /api/v1/opportunities/1 with invalid sourceUrl scheme returns 400 Bad Request")
    void updateOpportunity_withInvalidSourceUrl_returns400() throws Exception {
        UpdateOpportunityRequest request = new UpdateOpportunityRequest(
                "Title", "Desc", LocationType.REMOTE, null, null, null, null, null, null, "file:///etc/passwd"
        );

        mockMvc.perform(put("/api/v1/opportunities/1")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // 11. POST /api/v1/opportunities with non-existent company -> 404
    @Test
    @DisplayName("POST /api/v1/opportunities with non-existent company returns 404 Not Found")
    void createOpportunity_withNonExistentCompany_returns404() throws Exception {
        CreateOpportunityRequest request = new CreateOpportunityRequest(
                999L, "Title", "Desc", LocationType.REMOTE, null, null, null, null, null, null, null
        );

        when(opportunityService.createOpportunity(
                eq(999L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenThrow(new ResourceNotFoundException("Company not found with id: 999"));

        mockMvc.perform(post("/api/v1/opportunities")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // 12. POST /api/v1/opportunities mass-assignment defense ignores rogue fields
    @Test
    @DisplayName("POST /api/v1/opportunities with malicious payload binds only allowed fields")
    void createOpportunity_massAssignmentPayload_ignoresRogueFields() throws Exception {
        String roguePayload = """
            {
                "id": 999999,
                "companyId": 1,
                "title": "Legit Title",
                "createdByUser": {"id": 1},
                "createdByUserId": 1,
                "sourceProvider": "SYSTEM",
                "externalSourceId": "malicious-ext-id",
                "status": "EXPIRED",
                "rawHash": "malicious-hash",
                "createdAt": "2020-01-01T00:00:00Z"
            }
        """;

        when(opportunityService.createOpportunity(
                eq(1L),
                eq(101L), // principal is strictly preserved
                eq(SourceProvider.USER_MANUAL), // source provider is strictly server-controlled
                isNull(), // externalSourceId is null
                eq("Legit Title"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(sampleOpportunity);

        mockMvc.perform(post("/api/v1/opportunities")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roguePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        verify(opportunityService).createOpportunity(
                eq(1L), eq(101L), eq(SourceProvider.USER_MANUAL), isNull(), eq("Legit Title"),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()
        );
    }

    // 13. PUT /api/v1/opportunities/{id} without token -> 401
    @Test
    @DisplayName("PUT /api/v1/opportunities/{id} without token returns 401 Unauthorized")
    void updateOpportunity_withoutToken_returns401() throws Exception {
        UpdateOpportunityRequest request = new UpdateOpportunityRequest(
                "New Title", "New Desc", LocationType.HYBRID, null, null, null, null, null, null, null
        );

        mockMvc.perform(put("/api/v1/opportunities/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // 14. PUT /api/v1/opportunities/{id} by creator -> 200
    @Test
    @DisplayName("PUT /api/v1/opportunities/{id} by owner returns 200 OK")
    void updateOpportunity_byOwner_returns200() throws Exception {
        UpdateOpportunityRequest request = new UpdateOpportunityRequest(
                "Updated Title", "Updated Desc", LocationType.HYBRID, "Austin, TX",
                new BigDecimal("50.00"), "USD", StipendPeriod.HOURLY, 4, null, "https://stripe.com/job2"
        );

        sampleOpportunity.setTitle("Updated Title");
        when(opportunityService.updateOpportunity(
                eq(42L),
                eq(101L),
                eq("Updated Title"),
                eq("Updated Desc"),
                eq(LocationType.HYBRID),
                eq("Austin, TX"),
                eq(new BigDecimal("50.00")),
                eq("USD"),
                eq(StipendPeriod.HOURLY),
                eq(4),
                isNull(),
                eq("https://stripe.com/job2"),
                isNull() // status is null
        )).thenReturn(sampleOpportunity);

        mockMvc.perform(put("/api/v1/opportunities/42")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Title"))
                .andExpect(jsonPath("$.message").value("Opportunity updated successfully"));
    }

    // 15. PUT /api/v1/opportunities/{id} on another user's custom opp -> 403
    @Test
    @DisplayName("PUT /api/v1/opportunities/{id} on another user's custom opp returns 403 Forbidden")
    void updateOpportunity_onOtherUserOpp_returns403() throws Exception {
        UpdateOpportunityRequest request = new UpdateOpportunityRequest(
                "Hacked Title", null, null, null, null, null, null, null, null, null
        );

        when(opportunityService.updateOpportunity(
                eq(42L), eq(101L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenThrow(new AccessDeniedException("Unauthorized: Only the creator may modify this custom opportunity"));

        mockMvc.perform(put("/api/v1/opportunities/42")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // 16. PUT /api/v1/opportunities/{id} on global system opp -> 403
    @Test
    @DisplayName("PUT /api/v1/opportunities/{id} on global opp returns 403 Forbidden")
    void updateOpportunity_onGlobalOpp_returns403() throws Exception {
        UpdateOpportunityRequest request = new UpdateOpportunityRequest(
                "Hacked Global Title", null, null, null, null, null, null, null, null, null
        );

        when(opportunityService.updateOpportunity(
                eq(42L), eq(101L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenThrow(new AccessDeniedException("Cannot modify global system opportunities"));

        mockMvc.perform(put("/api/v1/opportunities/42")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // 17. PUT /api/v1/opportunities/{id} on non-existent opportunity -> 404
    @Test
    @DisplayName("PUT /api/v1/opportunities/{id} on non-existent returns 404 Not Found")
    void updateOpportunity_onNonExistent_returns404() throws Exception {
        UpdateOpportunityRequest request = new UpdateOpportunityRequest(
                "Title", null, null, null, null, null, null, null, null, null
        );

        when(opportunityService.updateOpportunity(
                eq(999L), eq(101L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenThrow(new ResourceNotFoundException("Opportunity not found with id: 999"));

        mockMvc.perform(put("/api/v1/opportunities/999")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // 18. DELETE /api/v1/opportunities/{id} without token -> 401
    @Test
    @DisplayName("DELETE /api/v1/opportunities/{id} without token returns 401 Unauthorized")
    void deleteOpportunity_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/opportunities/42"))
                .andExpect(status().isUnauthorized());
    }

    // 19. DELETE /api/v1/opportunities/{id} by creator -> 204
    @Test
    @DisplayName("DELETE /api/v1/opportunities/{id} by owner returns 204 No Content")
    void deleteOpportunity_byOwner_returns204() throws Exception {
        doNothing().when(opportunityService).deleteOpportunity(42L, 101L);

        mockMvc.perform(delete("/api/v1/opportunities/42")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());

        verify(opportunityService).deleteOpportunity(42L, 101L);
    }

    // 20. DELETE /api/v1/opportunities/{id} on another user's custom opp -> 403
    @Test
    @DisplayName("DELETE /api/v1/opportunities/{id} on another user's custom opp returns 403 Forbidden")
    void deleteOpportunity_onOtherUserOpp_returns403() throws Exception {
        doThrow(new AccessDeniedException("Unauthorized: Only the creator may delete this custom opportunity"))
                .when(opportunityService).deleteOpportunity(42L, 101L);

        mockMvc.perform(delete("/api/v1/opportunities/42")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // 21. DELETE /api/v1/opportunities/{id} on global system opp -> 403
    @Test
    @DisplayName("DELETE /api/v1/opportunities/{id} on global opp returns 403 Forbidden")
    void deleteOpportunity_onGlobalOpp_returns403() throws Exception {
        doThrow(new AccessDeniedException("Cannot delete global system opportunities"))
                .when(opportunityService).deleteOpportunity(42L, 101L);

        mockMvc.perform(delete("/api/v1/opportunities/42")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // 22. DELETE /api/v1/opportunities/{id} on non-existent opportunity -> 404
    @Test
    @DisplayName("DELETE /api/v1/opportunities/{id} on non-existent returns 404 Not Found")
    void deleteOpportunity_onNonExistent_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Opportunity not found with id: 999"))
                .when(opportunityService).deleteOpportunity(999L, 101L);

        mockMvc.perform(delete("/api/v1/opportunities/999")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // 23. DELETE /api/v1/opportunities/{id} with linked applications -> 409 Conflict
    @Test
    @DisplayName("DELETE /api/v1/opportunities/{id} with linked applications returns 409 Conflict")
    void deleteOpportunity_withLinkedApplications_returns409() throws Exception {
        SQLException sqlException = new SQLException("Cannot delete or update a parent row: a foreign key constraint fails (`interntrack_db`.`applications`, CONSTRAINT `fk_app_opportunity` FOREIGN KEY (`opportunity_id`) REFERENCES `opportunities` (`id`))", "23000", 1451);
        DataIntegrityViolationException dive = new DataIntegrityViolationException("FK violation", sqlException);

        doThrow(dive).when(opportunityService).deleteOpportunity(42L, 101L);

        mockMvc.perform(delete("/api/v1/opportunities/42")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cannot delete opportunity with existing candidate applications"));
    }

    // 24. GET /api/v1/opportunities/{id}/skill-match without token -> 401
    @Test
    @DisplayName("GET /api/v1/opportunities/{id}/skill-match without token returns 401 Unauthorized")
    void getOpportunitySkillMatch_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/opportunities/42/skill-match"))
                .andExpect(status().isUnauthorized());
    }

    // 25. GET /api/v1/opportunities/{id}/skill-match with token -> 200
    @Test
    @DisplayName("GET /api/v1/opportunities/{id}/skill-match with token returns 200 OK")
    void getOpportunitySkillMatch_withToken_returns200() throws Exception {
        com.interntrack.api.dto.skill.OpportunitySkillMatchResponse match = new com.interntrack.api.dto.skill.OpportunitySkillMatchResponse(
                42L, 75.0, "MEDIUM", List.of("Java"), List.of("Spring"), List.of("Docker"), List.of(), 2, 1, false
        );

        when(skillService.getOpportunitySkillMatch(101L, 42L)).thenReturn(match);

        mockMvc.perform(get("/api/v1/opportunities/42/skill-match")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.opportunityId").value(42))
                .andExpect(jsonPath("$.data.matchPercentage").value(75.0))
                .andExpect(jsonPath("$.data.matchLabel").value("MEDIUM"))
                .andExpect(jsonPath("$.message").value("Skill match computed successfully"));
    }

    // 26. GET /api/v1/opportunities/{id}/skill-match on non-existent -> 404
    @Test
    @DisplayName("GET /api/v1/opportunities/{id}/skill-match on non-existent returns 404 Not Found")
    void getOpportunitySkillMatch_notFound_returns404() throws Exception {
        when(skillService.getOpportunitySkillMatch(101L, 999L))
                .thenThrow(new ResourceNotFoundException("Opportunity not found with id: 999"));

        mockMvc.perform(get("/api/v1/opportunities/999/skill-match")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // 27. GET /api/v1/opportunities/{id}/skill-match Case D zero skills -> returns null matchPercentage and "No Skills Specified"
    @Test
    @DisplayName("GET /api/v1/opportunities/{id}/skill-match Case D zero skills returns null matchPercentage and 'No Skills Specified'")
    void getOpportunitySkillMatch_caseD_zeroSkills_returnsNullMatchPercentage() throws Exception {
        com.interntrack.api.dto.skill.OpportunitySkillMatchResponse match = new com.interntrack.api.dto.skill.OpportunitySkillMatchResponse(
                42L, null, "No Skills Specified", List.of(), List.of(), List.of(), List.of(), 0, 0, true
        );

        when(skillService.getOpportunitySkillMatch(101L, 42L)).thenReturn(match);

        mockMvc.perform(get("/api/v1/opportunities/42/skill-match")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.opportunityId").value(42))
                .andExpect(jsonPath("$.data.matchPercentage").doesNotExist())
                .andExpect(jsonPath("$.data.matchLabel").value("No Skills Specified"))
                .andExpect(jsonPath("$.data.zeroSkillsSpecified").value(true))
                .andExpect(jsonPath("$.message").value("Skill match computed successfully"));
    }
}