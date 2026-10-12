package com.interntrack.api.dto.document;

import com.interntrack.api.entity.enums.DocumentType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateDocumentRequest(
        @Size(max = 200, message = "Document title cannot exceed 200 characters")
        String title,

        DocumentType documentType,

        @Size(max = 1000, message = "Storage path cannot exceed 1000 characters")
        @Pattern(regexp = "^https?://.+", message = "Storage path must be a valid absolute HTTP or HTTPS URL")
        String storagePath,

        @Size(max = 50, message = "Version tag cannot exceed 50 characters")
        String versionTag
) {
}