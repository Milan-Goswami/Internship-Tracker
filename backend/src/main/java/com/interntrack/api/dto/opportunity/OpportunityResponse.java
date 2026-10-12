package com.interntrack.api.dto.opportunity;

import com.interntrack.api.entity.Opportunity;
import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.OpportunityStatus;
import com.interntrack.api.entity.enums.SourceProvider;
import com.interntrack.api.entity.enums.StipendPeriod;

import java.math.BigDecimal;
import java.time.Instant;

public record OpportunityResponse(
        Long id,
        Long companyId,
        String companyName,
        String companyLogoUrl,
        String title,
        String description,
        LocationType locationType,
        String locationCity,
        BigDecimal stipendAmount,
        String stipendCurrency,
        StipendPeriod stipendPeriod,
        Integer durationMonths,
        Instant applicationDeadline,
        String sourceUrl,
        OpportunityStatus status,
        SourceProvider sourceProvider,
        boolean isCustom,
        Instant createdAt,
        Instant updatedAt
) {
    public static OpportunityResponse from(Opportunity opportunity) {
        if (opportunity == null) {
            return null;
        }
        return new OpportunityResponse(
                opportunity.getId(),
                opportunity.getCompany() != null ? opportunity.getCompany().getId() : null,
                opportunity.getCompany() != null ? opportunity.getCompany().getName() : null,
                opportunity.getCompany() != null ? opportunity.getCompany().getLogoUrl() : null,
                opportunity.getTitle(),
                opportunity.getDescription(),
                opportunity.getLocationType(),
                opportunity.getLocationCity(),
                opportunity.getStipendAmount(),
                opportunity.getStipendCurrency(),
                opportunity.getStipendPeriod(),
                opportunity.getDurationMonths(),
                opportunity.getApplicationDeadline(),
                opportunity.getSourceUrl(),
                opportunity.getStatus(),
                opportunity.getSourceProvider(),
                opportunity.getCreatedByUser() != null,
                opportunity.getCreatedAt(),
                opportunity.getUpdatedAt()
        );
    }
}
