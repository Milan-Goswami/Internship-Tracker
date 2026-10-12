package com.interntrack.api.dto.document;

import com.interntrack.api.entity.Document;
import com.interntrack.api.entity.enums.DocumentType;

import java.time.Instant;

public record DocumentResponse(
        Long id,
        String title,
        DocumentType documentType,
        String storagePath,
        String versionTag,
        Instant uploadedAt
) {
    public static DocumentResponse from(Document document) {
        if (document == null) {
            return null;
        }
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getDocumentType(),
                document.getStoragePath(),
                document.getVersionTag(),
                document.getUploadedAt()
        );
    }
}