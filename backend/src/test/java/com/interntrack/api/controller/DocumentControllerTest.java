package com.interntrack.api.controller;

import com.interntrack.api.dto.document.CreateDocumentRequest;
import com.interntrack.api.dto.document.UpdateDocumentRequest;
import com.interntrack.api.entity.Document;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.DocumentType;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.JwtService;
import com.interntrack.api.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=TEST_DOCUMENT_CONTROLLER_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class DocumentControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private DocumentService documentService;

    private MockMvc mockMvc;
    private String userToken;
    private final Long USER_ID = 100L;

    private Document sampleDocument;

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

        sampleDocument = new Document(
                user,
                "Backend Resume 2026",
                DocumentType.RESUME,
                "https://drive.google.com/file/d/abcdef/view",
                "v1.0"
        );
        ReflectionTestUtils.setField(sampleDocument, "id", 42L);
        ReflectionTestUtils.setField(sampleDocument, "uploadedAt", Instant.parse("2026-09-01T10:00:00Z"));
    }

    // 1. GET /api/v1/documents
    @Test
    @DisplayName("GET /api/v1/documents without token returns 401")
    void listDocuments_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/documents with token returns 200 and document list")
    void listDocuments_withToken_returns200() throws Exception {
        when(documentService.findByUserId(USER_ID)).thenReturn(List.of(sampleDocument));

        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(42)))
                .andExpect(jsonPath("$.data[0].title", is("Backend Resume 2026")))
                .andExpect(jsonPath("$.data[0].documentType", is("RESUME")))
                .andExpect(jsonPath("$.data[0].storagePath", is("https://drive.google.com/file/d/abcdef/view")))
                .andExpect(jsonPath("$.data[0].versionTag", is("v1.0")))
                .andExpect(jsonPath("$.message", is("Documents retrieved successfully")));

        verify(documentService).findByUserId(USER_ID);
    }

    @Test
    @DisplayName("GET /api/v1/documents?documentType=RESUME filters by document type")
    void listDocuments_withTypeFilter_returnsFilteredList() throws Exception {
        when(documentService.findByUserIdAndDocumentType(USER_ID, DocumentType.RESUME))
                .thenReturn(List.of(sampleDocument));

        mockMvc.perform(get("/api/v1/documents")
                        .param("documentType", "RESUME")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].documentType", is("RESUME")));

        verify(documentService).findByUserIdAndDocumentType(USER_ID, DocumentType.RESUME);
    }

    // 2. POST /api/v1/documents
    @Test
    @DisplayName("POST /api/v1/documents without token returns 401")
    void createDocument_withoutToken_returns401() throws Exception {
        CreateDocumentRequest request = new CreateDocumentRequest(
                "Resume", DocumentType.RESUME, "https://example.com/res.pdf", "v1"
        );

        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/documents with valid body returns 201 with Location header")
    void createDocument_withValidBody_returns201() throws Exception {
        CreateDocumentRequest request = new CreateDocumentRequest(
                "Backend Resume 2026",
                DocumentType.RESUME,
                "https://drive.google.com/file/d/abcdef/view",
                "v1.0"
        );

        when(documentService.createDocument(
                eq(USER_ID),
                eq("Backend Resume 2026"),
                eq(DocumentType.RESUME),
                eq("https://drive.google.com/file/d/abcdef/view"),
                eq("v1.0")
        )).thenReturn(sampleDocument);

        mockMvc.perform(post("/api/v1/documents")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/documents/42"))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(42)))
                .andExpect(jsonPath("$.data.title", is("Backend Resume 2026")))
                .andExpect(jsonPath("$.message", is("Document registered successfully")));
    }

    @Test
    @DisplayName("POST /api/v1/documents with blank title returns 400")
    void createDocument_withBlankTitle_returns400() throws Exception {
        CreateDocumentRequest request = new CreateDocumentRequest(
                "   ", DocumentType.RESUME, "https://example.com/res.pdf", "v1"
        );

        mockMvc.perform(post("/api/v1/documents")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("Validation Failed")));
    }

    @Test
    @DisplayName("POST /api/v1/documents with null documentType returns 400")
    void createDocument_withNullType_returns400() throws Exception {
        String json = "{\"title\":\"Resume\",\"documentType\":null,\"storagePath\":\"https://example.com/res.pdf\"}";

        mockMvc.perform(post("/api/v1/documents")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/documents with invalid storagePath URL returns 400")
    void createDocument_withInvalidUrl_returns400() throws Exception {
        CreateDocumentRequest request = new CreateDocumentRequest(
                "Resume", DocumentType.RESUME, "invalid-url-format", "v1"
        );

        mockMvc.perform(post("/api/v1/documents")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // 3. GET /api/v1/documents/{id}
    @Test
    @DisplayName("GET /api/v1/documents/{id} when owned returns 200")
    void getDocumentById_whenOwned_returns200() throws Exception {
        when(documentService.findAccessibleById(42L, USER_ID)).thenReturn(sampleDocument);

        mockMvc.perform(get("/api/v1/documents/42")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(42)))
                .andExpect(jsonPath("$.data.title", is("Backend Resume 2026")));
    }

    @Test
    @DisplayName("GET /api/v1/documents/{id} when owned by another user returns 403 Forbidden")
    void getDocumentById_whenNonOwner_returns403() throws Exception {
        when(documentService.findAccessibleById(42L, USER_ID))
                .thenThrow(new AccessDeniedException("Unauthorized: User does not own this document"));

        mockMvc.perform(get("/api/v1/documents/42")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("GET /api/v1/documents/{id} when missing returns 404 Not Found")
    void getDocumentById_whenNotFound_returns404() throws Exception {
        when(documentService.findAccessibleById(99L, USER_ID))
                .thenThrow(new ResourceNotFoundException("Document not found with id: 99"));

        mockMvc.perform(get("/api/v1/documents/99")
                        .header("Authorization", userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    // 4. PUT /api/v1/documents/{id}
    @Test
    @DisplayName("PUT /api/v1/documents/{id} when valid returns 200")
    void updateDocument_whenValid_returns200() throws Exception {
        UpdateDocumentRequest request = new UpdateDocumentRequest(
                "Updated Resume Title",
                DocumentType.PORTFOLIO,
                "https://portfolio.dev/doc.pdf",
                "v2.0"
        );

        Document updated = new Document(
                sampleDocument.getUser(),
                "Updated Resume Title",
                DocumentType.PORTFOLIO,
                "https://portfolio.dev/doc.pdf",
                "v2.0"
        );
        ReflectionTestUtils.setField(updated, "id", 42L);
        ReflectionTestUtils.setField(updated, "uploadedAt", sampleDocument.getUploadedAt());

        when(documentService.updateDocument(
                eq(42L), eq(USER_ID), eq("Updated Resume Title"),
                eq(DocumentType.PORTFOLIO), eq("https://portfolio.dev/doc.pdf"), eq("v2.0")
        )).thenReturn(updated);

        mockMvc.perform(put("/api/v1/documents/42")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Updated Resume Title")))
                .andExpect(jsonPath("$.data.documentType", is("PORTFOLIO")))
                .andExpect(jsonPath("$.message", is("Document updated successfully")));
    }

    @Test
    @DisplayName("PUT /api/v1/documents/{id} when non-owner returns 403 Forbidden")
    void updateDocument_whenNonOwner_returns403() throws Exception {
        UpdateDocumentRequest request = new UpdateDocumentRequest(
                "Hacked Title", null, null, null
        );

        when(documentService.updateDocument(eq(42L), eq(USER_ID), any(), any(), any(), any()))
                .thenThrow(new AccessDeniedException("Unauthorized: User does not own this document"));

        mockMvc.perform(put("/api/v1/documents/42")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("PUT /api/v1/documents/{id} when missing returns 404 Not Found")
    void updateDocument_whenNotFound_returns404() throws Exception {
        UpdateDocumentRequest request = new UpdateDocumentRequest(
                "New Title", null, null, null
        );

        when(documentService.updateDocument(eq(99L), eq(USER_ID), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Document not found with id: 99"));

        mockMvc.perform(put("/api/v1/documents/99")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // 5. DELETE /api/v1/documents/{id}
    @Test
    @DisplayName("DELETE /api/v1/documents/{id} when owned returns 204 No Content")
    void deleteDocument_whenOwned_returns204() throws Exception {
        doNothing().when(documentService).deleteDocument(42L, USER_ID);

        mockMvc.perform(delete("/api/v1/documents/42")
                        .header("Authorization", userToken))
                .andExpect(status().isNoContent());

        verify(documentService).deleteDocument(42L, USER_ID);
    }

    @Test
    @DisplayName("DELETE /api/v1/documents/{id} when non-owner returns 403 Forbidden")
    void deleteDocument_whenNonOwner_returns403() throws Exception {
        doThrow(new AccessDeniedException("Unauthorized: User does not own this document"))
                .when(documentService).deleteDocument(42L, USER_ID);

        mockMvc.perform(delete("/api/v1/documents/42")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("DELETE /api/v1/documents/{id} when missing returns 404 Not Found")
    void deleteDocument_whenNotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Document not found with id: 99"))
                .when(documentService).deleteDocument(99L, USER_ID);

        mockMvc.perform(delete("/api/v1/documents/99")
                        .header("Authorization", userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }
}