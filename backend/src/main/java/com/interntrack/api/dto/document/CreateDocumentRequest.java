package com.interntrack.api.dto.document;

import com.interntrack.api.entity.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDocumentRequest(
        @NotBlank(message = "Document title cannot be blank")
        @Size(max = 200, message = "Document title cannot exceed 200 characters")
        String title,

        @NotNull(message = "Document type is required")
        DocumentType documentType,

        @NotBlank(message = "Storage path cannot be blank")
        @Size(max = 1000, message = "Storage path cannot exceed 1000 characters")
        @Pattern(regexp = "^https?://.+", message = "Storage path must be a valid absolute HTTP or HTTPS URL")
        String storagePath,

        @Size(max = 50, message = "Version tag cannot exceed 50 characters")
        String versionTag
) {
}