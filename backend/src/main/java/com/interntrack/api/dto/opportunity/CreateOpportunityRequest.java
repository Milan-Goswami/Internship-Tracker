package com.interntrack.api.dto.opportunity;

import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.StipendPeriod;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateOpportunityRequest(
        @NotNull(message = "Company ID is required")
        Long companyId,

        @NotBlank(message = "Opportunity title is required")
        @Size(max = 200, message = "Opportunity title must not exceed 200 characters")
        String title,

        @Size(max = 10000, message = "Description must not exceed 10000 characters")
        String description,

        LocationType locationType,

        @Size(max = 150, message = "Location city must not exceed 150 characters")
        String locationCity,

        @DecimalMin(value = "0.0", inclusive = true, message = "Stipend amount must be non-negative")
        BigDecimal stipendAmount,

        @Size(max = 10, message = "Stipend currency must not exceed 10 characters")
        String stipendCurrency,

        StipendPeriod stipendPeriod,

        @Min(value = 1, message = "Duration must be at least 1 month")
        @Max(value = 24, message = "Duration must not exceed 24 months")
        Integer durationMonths,

        Instant applicationDeadline,

        @Size(max = 1000, message = "Source URL must not exceed 1000 characters")
        @Pattern(regexp = "^(\\s*|https?://.+)$", message = "Source URL must be a valid absolute HTTP or HTTPS URL")
        String sourceUrl
) {
}
