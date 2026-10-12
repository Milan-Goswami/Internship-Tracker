package com.interntrack.api.dto.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(
        @NotBlank(message = "Company name is required")
        @Size(max = 150, message = "Company name must not exceed 150 characters")
        String name,

        @Size(max = 100, message = "Domain must not exceed 100 characters")
        String domain,

        @Size(max = 100, message = "Industry must not exceed 100 characters")
        String industry,

        @Size(max = 500, message = "Logo URL must not exceed 500 characters")
        @Pattern(regexp = "^(\\s*|https?://.+)$", message = "Logo URL must be a valid absolute HTTP or HTTPS URL")
        String logoUrl,

        @Size(max = 150, message = "Headquarters location must not exceed 150 characters")
        String headquartersLocation
) {
}
