package com.interntrack.api.dto.company;

import com.interntrack.api.entity.Company;

import java.time.Instant;

public record CompanyResponse(
        Long id,
        String name,
        String canonicalName,
        String domain,
        String industry,
        String logoUrl,
        String headquartersLocation,
        Instant createdAt
) {
    public static CompanyResponse from(Company company) {
        if (company == null) {
            return null;
        }
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getCanonicalName(),
                company.getDomain(),
                company.getIndustry(),
                company.getLogoUrl(),
                company.getHeadquartersLocation(),
                company.getCreatedAt()
        );
    }
}
