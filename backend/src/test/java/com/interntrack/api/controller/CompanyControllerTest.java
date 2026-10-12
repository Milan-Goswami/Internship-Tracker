package com.interntrack.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interntrack.api.dto.company.CreateCompanyRequest;
import com.interntrack.api.entity.Company;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.CompanyAlreadyExistsException;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.JwtService;
import com.interntrack.api.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=TEST_COMPANY_CONTROLLER_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class CompanyControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CompanyService companyService;

    private MockMvc mockMvc;
    private Company sampleCompany;
    private AuthenticatedUser samplePrincipal;
    private String validToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        sampleCompany = new Company("Google LLC", "google.com", "Technology");
        sampleCompany.setCanonicalName("googlellc");
        sampleCompany.setLogoUrl("https://example.com/google.png");
        sampleCompany.setHeadquartersLocation("Mountain View, CA");
        ReflectionTestUtils.setField(sampleCompany, "id", 10L);
        ReflectionTestUtils.setField(sampleCompany, "createdAt", Instant.parse("2026-01-01T12:00:00Z"));

        samplePrincipal = new AuthenticatedUser(101L, "candidate@example.com", "Alice Candidate", UserRole.ROLE_USER, true);
        validToken = jwtService.generateAccessToken(samplePrincipal);
    }

    // 1. GET /api/v1/companies/{id} without token -> 401
    @Test
    @DisplayName("GET /api/v1/companies/{id} without token returns 401 Unauthorized")
    void getCompany_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/companies/10"))
                .andExpect(status().isUnauthorized());
    }

    // 2. GET /api/v1/companies/{id} with valid token -> 200
    @Test
    @DisplayName("GET /api/v1/companies/{id} with valid token returns 200 OK and company data")
    void getCompany_withValidToken_returns200() throws Exception {
        when(companyService.findById(10L)).thenReturn(Optional.of(sampleCompany));

        mockMvc.perform(get("/api/v1/companies/10")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("Google LLC"))
                .andExpect(jsonPath("$.data.canonicalName").value("googlellc"))
                .andExpect(jsonPath("$.data.domain").value("google.com"))
                .andExpect(jsonPath("$.data.industry").value("Technology"))
                .andExpect(jsonPath("$.data.logoUrl").value("https://example.com/google.png"))
                .andExpect(jsonPath("$.data.headquartersLocation").value("Mountain View, CA"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.message").value("Company retrieved successfully"));

        verify(companyService).findById(10L);
    }

    // 3. GET /api/v1/companies/{id} when missing -> 404
    @Test
    @DisplayName("GET /api/v1/companies/{id} when missing returns 404 Not Found")
    void getCompany_whenMissing_returns404() throws Exception {
        when(companyService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/companies/999")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Company not found with id: 999"));
    }

    // 4. GET /api/v1/companies without token -> 401
    @Test
    @DisplayName("GET /api/v1/companies without token returns 401 Unauthorized")
    void listCompanies_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/companies"))
                .andExpect(status().isUnauthorized());
    }

    // 5. GET /api/v1/companies with valid token -> 200
    @Test
    @DisplayName("GET /api/v1/companies with valid token returns 200 OK and company list")
    void listCompanies_withValidToken_returns200() throws Exception {
        when(companyService.findAll()).thenReturn(List.of(sampleCompany));

        mockMvc.perform(get("/api/v1/companies")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].name").value("Google LLC"))
                .andExpect(jsonPath("$.data[0].canonicalName").value("googlellc"))
                .andExpect(jsonPath("$.message").value("Companies retrieved successfully"));

        verify(companyService).findAll();
    }

    // 6. POST /api/v1/companies without token -> 401
    @Test
    @DisplayName("POST /api/v1/companies without token returns 401 Unauthorized")
    void createCompany_withoutToken_returns401() throws Exception {
        CreateCompanyRequest request = new CreateCompanyRequest("Stripe", "stripe.com", "Fintech", null, "San Francisco");

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // 7. POST /api/v1/companies valid payload -> 201
    @Test
    @DisplayName("POST /api/v1/companies valid payload returns 201 Created and company data")
    void createCompany_validPayload_returns201() throws Exception {
        CreateCompanyRequest request = new CreateCompanyRequest(
                "Stripe, Inc.",
                "stripe.com",
                "Fintech",
                "https://logo.clearbit.com/stripe.com",
                "San Francisco, CA"
        );

        Company createdCompany = new Company("Stripe, Inc.", "stripe.com", "Fintech");
        createdCompany.setCanonicalName("stripeinc");
        createdCompany.setLogoUrl("https://logo.clearbit.com/stripe.com");
        createdCompany.setHeadquartersLocation("San Francisco, CA");
        ReflectionTestUtils.setField(createdCompany, "id", 20L);
        ReflectionTestUtils.setField(createdCompany, "createdAt", Instant.now());

        when(companyService.createCompany(
                eq("Stripe, Inc."),
                eq("stripe.com"),
                eq("Fintech"),
                eq("https://logo.clearbit.com/stripe.com"),
                eq("San Francisco, CA")
        )).thenReturn(createdCompany);

        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(20))
                .andExpect(jsonPath("$.data.name").value("Stripe, Inc."))
                .andExpect(jsonPath("$.data.canonicalName").value("stripeinc"))
                .andExpect(jsonPath("$.data.domain").value("stripe.com"))
                .andExpect(jsonPath("$.message").value("Company created successfully"));
    }

    // 8. POST /api/v1/companies duplicate canonical name -> 409
    @Test
    @DisplayName("POST /api/v1/companies duplicate canonical name returns 409 Conflict")
    void createCompany_duplicateCanonicalName_returns409() throws Exception {
        CreateCompanyRequest request = new CreateCompanyRequest("Google, LLC!", "google.com", "Technology", null, null);

        when(companyService.createCompany(any(), any(), any(), any(), any()))
                .thenThrow(new CompanyAlreadyExistsException("Company with canonical name 'googlellc' already exists"));

        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Company with canonical name 'googlellc' already exists"));
    }

    // 9. POST /api/v1/companies blank name -> 400
    @Test
    @DisplayName("POST /api/v1/companies blank name returns 400 Bad Request")
    void createCompany_blankName_returns400() throws Exception {
        String invalidJson = "{\"name\":\"\"}";

        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/v1/companies invalid logoUrl returns 400 Bad Request")
    void createCompany_invalidLogoUrl_returns400() throws Exception {
        String invalidJson = "{\"name\":\"Test Company\", \"logoUrl\":\"javascript:alert(1)\"}";

        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // 10. POST /api/v1/companies whitespace name -> 400
    @Test
    @DisplayName("POST /api/v1/companies whitespace-only name returns 400 Bad Request")
    void createCompany_whitespaceName_returns400() throws Exception {
        String invalidJson = "{\"name\":\"   \"}";

        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // 11. POST /api/v1/companies punctuation only name -> 400
    @Test
    @DisplayName("POST /api/v1/companies punctuation-only name returns 400 Bad Request")
    void createCompany_punctuationOnlyName_returns400() throws Exception {
        CreateCompanyRequest request = new CreateCompanyRequest("!@#$$%", null, null, null, null);

        when(companyService.createCompany(eq("!@#$$%"), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Company name must contain alphanumeric characters"));

        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Company name must contain alphanumeric characters"));
    }

    // 12. POST /api/v1/companies malicious immutable fields -> cannot control entity
    @Test
    @DisplayName("POST /api/v1/companies malicious immutable fields cannot control entity")
    void createCompany_maliciousImmutableFields_cannotControlEntity() throws Exception {
        String maliciousPayload = "{" +
                "\"id\": 999999," +
                "\"name\": \"Google\"," +
                "\"canonicalName\": \"attacker\"," +
                "\"createdAt\": \"2000-01-01T00:00:00Z\"," +
                "\"updatedAt\": \"2000-01-01T00:00:00Z\"," +
                "\"domain\": \"google.com\"," +
                "\"industry\": \"Technology\"," +
                "\"logoUrl\": \"https://example.com/logo.png\"," +
                "\"headquartersLocation\": \"Mountain View\"" +
                "}";

        Company safeCreated = new Company("Google", "google.com", "Technology");
        safeCreated.setCanonicalName("google");
        safeCreated.setLogoUrl("https://example.com/logo.png");
        safeCreated.setHeadquartersLocation("Mountain View");
        ReflectionTestUtils.setField(safeCreated, "id", 10L);
        ReflectionTestUtils.setField(safeCreated, "createdAt", Instant.parse("2026-09-04T12:00:00Z"));

        when(companyService.createCompany(
                eq("Google"),
                eq("google.com"),
                eq("Technology"),
                eq("https://example.com/logo.png"),
                eq("Mountain View")
        )).thenReturn(safeCreated);

        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.canonicalName").value("google"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-09-04T12:00:00Z"))
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist());

        verify(companyService).createCompany("Google", "google.com", "Technology", "https://example.com/logo.png", "Mountain View");
    }

    // 13. POST /api/v1/companies optional blank fields are sanitized
    @Test
    @DisplayName("POST /api/v1/companies optional blank fields are sanitized to null by service")
    void createCompany_optionalBlankFields_areSanitized() throws Exception {
        CreateCompanyRequest request = new CreateCompanyRequest("Acme Corp", "   ", "", "   ", " ");

        Company sanitized = new Company("Acme Corp", null, null);
        sanitized.setCanonicalName("acmecorp");
        ReflectionTestUtils.setField(sanitized, "id", 30L);
        ReflectionTestUtils.setField(sanitized, "createdAt", Instant.now());

        when(companyService.createCompany(eq("Acme Corp"), eq("   "), eq(""), eq("   "), eq(" ")))
                .thenReturn(sanitized);

        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.domain").doesNotExist())
                .andExpect(jsonPath("$.data.industry").doesNotExist())
                .andExpect(jsonPath("$.data.logoUrl").doesNotExist())
                .andExpect(jsonPath("$.data.headquartersLocation").doesNotExist());
    }

    // 14. CompanyResponse does not expose internal fields
    @Test
    @DisplayName("CompanyResponse does not expose internal sensitive or security fields")
    void companyResponse_doesNotExposeInternalFields() throws Exception {
        when(companyService.findById(10L)).thenReturn(Optional.of(sampleCompany));

        mockMvc.perform(get("/api/v1/companies/10")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.role").doesNotExist())
                .andExpect(jsonPath("$.data.user").doesNotExist())
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.createdByUser").doesNotExist());
    }

    // 15. GET /api/v1/companies with search query -> 200 filtered
    @Test
    @DisplayName("GET /api/v1/companies with search query returns filtered companies")
    void listCompanies_withSearchQuery_returnsFilteredCompanies() throws Exception {
        when(companyService.searchByName("google")).thenReturn(List.of(sampleCompany));

        mockMvc.perform(get("/api/v1/companies")
                        .param("search", "google")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Google LLC"));

        verify(companyService).searchByName("google");
        verify(companyService, never()).findAll();
    }

    // 16. Security regression: GET /api/v1/users/me without token -> 401
    @Test
    @DisplayName("Security Regression: GET /api/v1/users/me without token returns 401")
    void securityRegression_getUsersMeWithoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // 17. Security regression: PATCH /api/v1/users/me without token -> 401
    @Test
    @DisplayName("Security Regression: PATCH /api/v1/users/me without token returns 401")
    void securityRegression_patchUsersMeWithoutToken_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"New Name\"}"))
                .andExpect(status().isUnauthorized());
    }

    // 18. Security regression: GET /api/v1/health without token -> 200
    @Test
    @DisplayName("Security Regression: GET /api/v1/health without token returns 200")
    void securityRegression_getHealthWithoutToken_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }

    // 19. DataIntegrityViolation on uk_company_canonical constraint -> 409 Conflict
    @Test
    @DisplayName("POST /api/v1/companies when DataIntegrityViolationException on uk_company_canonical occurs returns 409 Conflict")
    void createCompany_databaseConstraintViolation_returns409() throws Exception {
        CreateCompanyRequest request = new CreateCompanyRequest("Stripe, Inc.", "stripe.com", "Fintech", null, null);

        SQLException sqlException = new SQLException("Duplicate entry 'stripeinc' for key 'companies.uk_company_canonical'");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("Constraint violation", sqlException);

        when(companyService.createCompany(any(), any(), any(), any(), any()))
                .thenThrow(dive);

        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Company with this canonical name already exists"));
    }

    // 20. Unrelated DataIntegrityViolation does NOT claim company already exists
    @Test
    @DisplayName("POST /api/v1/companies when unrelated DataIntegrityViolationException occurs does not claim company already exists")
    void createCompany_unrelatedDataIntegrityViolation_doesNotClaimCompanyExists() throws Exception {
        CreateCompanyRequest request = new CreateCompanyRequest("Acme Corp", "acme.com", "Tech", null, null);

        SQLException sqlException = new SQLException("Cannot add or update a child row: a foreign key constraint fails");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("Foreign key violation", sqlException);

        when(companyService.createCompany(any(), any(), any(), any(), any()))
                .thenThrow(dive);

        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Database constraint violation occurred"));
    }
}