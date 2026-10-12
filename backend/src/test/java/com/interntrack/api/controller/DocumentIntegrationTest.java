package com.interntrack.api.controller;

import com.interntrack.api.dto.document.CreateDocumentRequest;
import com.interntrack.api.dto.document.UpdateDocumentRequest;
import com.interntrack.api.entity.Document;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.DocumentType;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.repository.DocumentRepository;
import com.interntrack.api.repository.UserRepository;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=DOCUMENT_INTEGRATION_TEST_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class DocumentIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Create User A
        userA = new User("cand_a_" + System.currentTimeMillis() + "@test.com", "hashA", "Candidate A", UserRole.ROLE_USER);
        userA = userRepository.save(userA);
        AuthenticatedUser authA = new AuthenticatedUser(userA.getId(), userA.getEmail(), userA.getFullName(), userA.getRole(), true);
        tokenA = "Bearer " + jwtService.generateAccessToken(authA);

        // Create User B
        userB = new User("cand_b_" + System.currentTimeMillis() + "@test.com", "hashB", "Candidate B", UserRole.ROLE_USER);
        userB = userRepository.save(userB);
        AuthenticatedUser authB = new AuthenticatedUser(userB.getId(), userB.getEmail(), userB.getFullName(), userB.getRole(), true);
        tokenB = "Bearer " + jwtService.generateAccessToken(authB);
    }

    @AfterEach
    void tearDown() {
        documentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("End-to-end multi-tenant lifecycle and IDOR verification")
    void fullDocumentLifecycle_andIdorProtection() throws Exception {
        // 1. User A creates document
        CreateDocumentRequest createReq = new CreateDocumentRequest(
                "Resume Fall 2026",
                DocumentType.RESUME,
                "https://drive.google.com/file/d/fall2026/view",
                "v1.0"
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/documents")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/documents/")))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Resume Fall 2026")))
                .andExpect(jsonPath("$.data.documentType", is("RESUME")))
                .andReturn();

        Long docId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        assertThat(docId).isNotNull();

        // 2. User A retrieves their document
        mockMvc.perform(get("/api/v1/documents/" + docId)
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(docId.intValue())));

        // 3. User A lists documents
        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 4. User B lists documents -> receives empty list (tenant isolation)
        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(0)));

        // 5. IDOR: User B attempts GET on User A's document -> 403 Forbidden
        mockMvc.perform(get("/api/v1/documents/" + docId)
                        .header("Authorization", tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("Forbidden")));

        // 6. IDOR: User B attempts PUT on User A's document -> 403 Forbidden
        UpdateDocumentRequest updateReqB = new UpdateDocumentRequest("Malicious Title", null, null, null);
        mockMvc.perform(put("/api/v1/documents/" + docId)
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReqB)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));

        // 7. IDOR: User B attempts DELETE on User A's document -> 403 Forbidden
        mockMvc.perform(delete("/api/v1/documents/" + docId)
                        .header("Authorization", tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));

        // 8. User A updates their document
        UpdateDocumentRequest updateReqA = new UpdateDocumentRequest(
                "Resume Fall 2026 Final",
                DocumentType.RESUME,
                "https://drive.google.com/file/d/fall2026_final/view",
                "v1.1"
        );
        mockMvc.perform(put("/api/v1/documents/" + docId)
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReqA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Resume Fall 2026 Final")))
                .andExpect(jsonPath("$.data.versionTag", is("v1.1")));

        // Verify in DB
        Document inDb = documentRepository.findById(docId).orElseThrow();
        assertThat(inDb.getTitle()).isEqualTo("Resume Fall 2026 Final");
        assertThat(inDb.getVersionTag()).isEqualTo("v1.1");

        // 9. User A deletes their document -> 204 No Content
        mockMvc.perform(delete("/api/v1/documents/" + docId)
                        .header("Authorization", tokenA))
                .andExpect(status().isNoContent());

        // Verify deleted from DB
        assertThat(documentRepository.findById(docId)).isEmpty();
    }
}