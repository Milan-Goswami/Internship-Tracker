package com.interntrack.api.service;

import com.interntrack.api.entity.Document;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.DocumentType;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.DocumentRepository;
import com.interntrack.api.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public DocumentService(DocumentRepository documentRepository,
                           UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    /**
     * Looks up a document by its primary key ID.
     */
    @Transactional(readOnly = true)
    public Optional<Document> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return documentRepository.findById(id);
    }

    /**
     * Retrieves an owned document by ID, throwing AccessDeniedException if owned by another user,
     * or ResourceNotFoundException if not found.
     */
    @Transactional(readOnly = true)
    public Document findAccessibleById(Long documentId, Long userId) {
        if (documentId == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        if (document.getUser() == null || !Objects.equals(document.getUser().getId(), userId)) {
            throw new AccessDeniedException("Unauthorized: User does not own this document");
        }

        return document;
    }

    /**
     * Retrieves all documents belonging to a user.
     */
    @Transactional(readOnly = true)
    public List<Document> findByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return documentRepository.findByUserId(userId);
    }

    /**
     * Retrieves documents belonging to a user filtered by document type.
     */
    @Transactional(readOnly = true)
    public List<Document> findByUserIdAndDocumentType(Long userId, DocumentType documentType) {
        if (userId == null || documentType == null) {
            return List.of();
        }
        return documentRepository.findByUserIdAndDocumentType(userId, documentType);
    }

    /**
     * Creates and persists new document metadata referencing an external document URL.
     *
     * @param userId       mandatory owner user ID
     * @param title        mandatory document title
     * @param documentType mandatory document type (RESUME, COVER_LETTER, etc.)
     * @param storagePath  mandatory valid absolute HTTP/HTTPS URL
     * @param versionTag   optional version identifier (e.g. "v1", "2026-Fall")
     * @return persisted Document entity
     * @throws ResourceNotFoundException if user not found
     * @throws IllegalArgumentException if fields are invalid or storagePath is not a valid HTTP/HTTPS URL
     */
    @Transactional
    public Document createDocument(
            Long userId,
            String title,
            DocumentType documentType,
            String storagePath,
            String versionTag) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Document title cannot be null or blank");
        }
        if (documentType == null) {
            throw new IllegalArgumentException("Document type cannot be null");
        }
        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("Storage path cannot be null or blank");
        }
        if (!isValidHttpUrl(storagePath)) {
            throw new IllegalArgumentException("Storage path must be a valid absolute HTTP or HTTPS URL");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Document document = new Document();
        document.setUser(user);
        document.setTitle(title.trim());
        document.setDocumentType(documentType);
        document.setStoragePath(storagePath.trim());
        document.setVersionTag(versionTag != null && !versionTag.isBlank() ? versionTag.trim() : null);

        return documentRepository.save(document);
    }

    /**
     * Updates mutable fields of existing document metadata.
     */
    @Transactional
    public Document updateDocument(
            Long documentId,
            Long userId,
            String title,
            DocumentType documentType,
            String storagePath,
            String versionTag) {

        if (documentId == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        if (document.getUser() == null || !Objects.equals(document.getUser().getId(), userId)) {
            throw new AccessDeniedException("Unauthorized: User does not own this document");
        }

        if (title != null && !title.isBlank()) {
            document.setTitle(title.trim());
        }
        if (documentType != null) {
            document.setDocumentType(documentType);
        }
        if (storagePath != null && !storagePath.isBlank()) {
            if (!isValidHttpUrl(storagePath)) {
                throw new IllegalArgumentException("Storage path must be a valid absolute HTTP or HTTPS URL");
            }
            document.setStoragePath(storagePath.trim());
        }
        if (versionTag != null) {
            document.setVersionTag(!versionTag.isBlank() ? versionTag.trim() : null);
        }

        return documentRepository.save(document);
    }

    /**
     * Deletes document metadata belonging to the specified user.
     */
    @Transactional
    public void deleteDocument(Long documentId, Long userId) {
        if (documentId == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        if (document.getUser() == null || !Objects.equals(document.getUser().getId(), userId)) {
            throw new AccessDeniedException("Unauthorized: User does not own this document");
        }

        documentRepository.delete(document);
    }

    /**
     * Validates whether a storage path is a syntactically valid absolute HTTP or HTTPS URL.
     */
    public boolean isValidHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null && !uri.getHost().isBlank();
        } catch (Exception e) {
            return false;
        }
    }
}