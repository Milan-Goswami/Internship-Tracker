package com.interntrack.api.controller;

import com.interntrack.api.dto.document.CreateDocumentRequest;
import com.interntrack.api.dto.document.DocumentResponse;
import com.interntrack.api.dto.document.UpdateDocumentRequest;
import com.interntrack.api.entity.Document;
import com.interntrack.api.entity.enums.DocumentType;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listDocuments(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) DocumentType documentType) {

        List<Document> documents;
        if (documentType != null) {
            documents = documentService.findByUserIdAndDocumentType(authenticatedUser.id(), documentType);
        } else {
            documents = documentService.findByUserId(authenticatedUser.id());
        }

        List<DocumentResponse> responseList = documents.stream()
                .map(DocumentResponse::from)
                .toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", responseList,
                "message", "Documents retrieved successfully"
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createDocument(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateDocumentRequest request) {

        Document created = documentService.createDocument(
                authenticatedUser.id(),
                request.title(),
                request.documentType(),
                request.storagePath(),
                request.versionTag()
        );

        URI location = URI.create("/api/v1/documents/" + created.getId());

        return ResponseEntity.created(location).body(Map.of(
                "success", true,
                "data", DocumentResponse.from(created),
                "message", "Document registered successfully"
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDocumentById(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id) {

        Document document = documentService.findAccessibleById(id, authenticatedUser.id());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", DocumentResponse.from(document),
                "message", "Document retrieved successfully"
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDocument(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateDocumentRequest request) {

        Document updated = documentService.updateDocument(
                id,
                authenticatedUser.id(),
                request.title(),
                request.documentType(),
                request.storagePath(),
                request.versionTag()
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", DocumentResponse.from(updated),
                "message", "Document updated successfully"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id) {

        documentService.deleteDocument(id, authenticatedUser.id());
        return ResponseEntity.noContent().build();
    }
}