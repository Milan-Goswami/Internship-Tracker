package com.interntrack.api.service;

import com.interntrack.api.entity.Company;
import com.interntrack.api.entity.Opportunity;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.OpportunityStatus;
import com.interntrack.api.entity.enums.SourceProvider;
import com.interntrack.api.entity.enums.StipendPeriod;
import com.interntrack.api.exception.OpportunityAlreadyExistsException;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.CompanyRepository;
import com.interntrack.api.repository.OpportunityRepository;
import com.interntrack.api.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public OpportunityService(OpportunityRepository opportunityRepository,
                              CompanyRepository companyRepository,
                              UserRepository userRepository) {
        this.opportunityRepository = opportunityRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    /**
     * Looks up an opportunity by primary key ID.
     */
    @Transactional(readOnly = true)
    public Optional<Opportunity> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return opportunityRepository.findById(id);
    }

    /**
     * Finds an opportunity accessible by the user (Global or user's custom).
     */
    @Transactional(readOnly = true)
    public Optional<Opportunity> findAccessibleById(Long id, Long userId) {
        if (id == null || userId == null) {
            return Optional.empty();
        }
        return opportunityRepository.findAccessibleById(id, userId);
    }

    /**
     * Lists all accessible opportunities for a user (Global + user's custom).
     */
    @Transactional(readOnly = true)
    public List<Opportunity> findAccessibleOpportunities(Long userId, LocationType locationType, String search) {
        if (userId == null) {
            return List.of();
        }
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return opportunityRepository.findAccessibleOpportunities(userId, locationType, cleanSearch);
    }

    /**
     * Looks up an opportunity by external provider identity (used by ingestion adapters).
     */
    @Transactional(readOnly = true)
    public Optional<Opportunity> findByProviderAndExternalId(SourceProvider sourceProvider, String externalSourceId) {
        if (sourceProvider == null || externalSourceId == null || externalSourceId.isBlank()) {
            return Optional.empty();
        }
        return opportunityRepository.findBySourceProviderAndExternalSourceId(sourceProvider, externalSourceId.trim());
    }

    /**
     * Looks up an opportunity by SHA-256 deduplication hash.
     */
    @Transactional(readOnly = true)
    public Optional<Opportunity> findByRawHash(String rawHash) {
        if (rawHash == null || rawHash.isBlank()) {
            return Optional.empty();
        }
        return opportunityRepository.findByRawHash(rawHash.trim());
    }

    /**
     * Lists opportunities matching a status filter (e.g. ACTIVE).
     */
    @Transactional(readOnly = true)
    public List<Opportunity> findByStatus(OpportunityStatus status) {
        if (status == null) {
            return List.of();
        }
        return opportunityRepository.findByStatus(status);
    }

    /**
     * Creates and persists a new Opportunity (Global System or User-Custom).
     */
    @Transactional
    public Opportunity createOpportunity(
            Long companyId,
            Long createdByUserId,
            SourceProvider sourceProvider,
            String externalSourceId,
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
            String rawHash) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Opportunity title cannot be null or blank");
        }
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID cannot be null");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + companyId));

        User creatorUser = null;
        if (createdByUserId != null) {
            creatorUser = userRepository.findById(createdByUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Creator user not found with id: " + createdByUserId));
        }

        SourceProvider effectiveProvider = sourceProvider != null ? sourceProvider : SourceProvider.SYSTEM;
        String cleanExternalId = externalSourceId != null && !externalSourceId.isBlank() ? externalSourceId.trim() : null;

        if (cleanExternalId != null) {
            if (opportunityRepository.findBySourceProviderAndExternalSourceId(effectiveProvider, cleanExternalId).isPresent()) {
                throw new OpportunityAlreadyExistsException(
                        "Opportunity with provider '" + effectiveProvider + "' and external ID '" + cleanExternalId + "' already exists"
                );
            }
        }

        Opportunity opportunity = new Opportunity();
        opportunity.setCompany(company);
        opportunity.setCreatedByUser(creatorUser);
        opportunity.setSourceProvider(effectiveProvider);
        opportunity.setExternalSourceId(cleanExternalId);
        opportunity.setTitle(title.trim());
        opportunity.setDescription(description != null && !description.isBlank() ? description.trim() : null);
        opportunity.setLocationType(locationType != null ? locationType : LocationType.REMOTE);
        opportunity.setLocationCity(locationCity != null && !locationCity.isBlank() ? locationCity.trim() : null);
        opportunity.setStipendAmount(stipendAmount);
        opportunity.setStipendCurrency(stipendCurrency != null && !stipendCurrency.isBlank() ? stipendCurrency.trim().toUpperCase() : "USD");
        opportunity.setStipendPeriod(stipendPeriod != null ? stipendPeriod : StipendPeriod.HOURLY);
        opportunity.setDurationMonths(durationMonths);
        opportunity.setApplicationDeadline(applicationDeadline);
        opportunity.setSourceUrl(sourceUrl != null && !sourceUrl.isBlank() ? sourceUrl.trim() : null);
        opportunity.setRawHash(rawHash != null && !rawHash.isBlank() ? rawHash.trim() : null);
        opportunity.setStatus(OpportunityStatus.ACTIVE);

        return opportunityRepository.save(opportunity);
    }

    /**
     * Updates mutable fields of an existing opportunity.
     * Enforces immutability on provider identity, creator ownership, status, and company association.
     */
    @Transactional
    public Opportunity updateOpportunity(
            Long id,
            Long requesterUserId,
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
            OpportunityStatus status) {

        if (id == null) {
            throw new IllegalArgumentException("Opportunity ID cannot be null");
        }

        Opportunity opportunity = opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + id));

        // Global system opportunities cannot be modified by standard users
        if (opportunity.getCreatedByUser() == null) {
            throw new AccessDeniedException("Cannot modify global system opportunities");
        }

        // Ownership enforcement for user-custom opportunities
        if (requesterUserId == null || !Objects.equals(opportunity.getCreatedByUser().getId(), requesterUserId)) {
            throw new AccessDeniedException("Unauthorized: Only the creator may modify this custom opportunity");
        }

        if (title != null && !title.isBlank()) {
            opportunity.setTitle(title.trim());
        }
        if (description != null) {
            opportunity.setDescription(!description.isBlank() ? description.trim() : null);
        }
        if (locationType != null) {
            opportunity.setLocationType(locationType);
        }
        if (locationCity != null) {
            opportunity.setLocationCity(!locationCity.isBlank() ? locationCity.trim() : null);
        }
        if (stipendAmount != null) {
            opportunity.setStipendAmount(stipendAmount);
        }
        if (stipendCurrency != null && !stipendCurrency.isBlank()) {
            opportunity.setStipendCurrency(stipendCurrency.trim().toUpperCase());
        }
        if (stipendPeriod != null) {
            opportunity.setStipendPeriod(stipendPeriod);
        }
        if (durationMonths != null) {
            opportunity.setDurationMonths(durationMonths);
        }
        if (applicationDeadline != null) {
            opportunity.setApplicationDeadline(applicationDeadline);
        }
        if (sourceUrl != null) {
            opportunity.setSourceUrl(!sourceUrl.isBlank() ? sourceUrl.trim() : null);
        }
        if (status != null) {
            opportunity.setStatus(status);
        }

        return opportunityRepository.save(opportunity);
    }

    /**
     * Deletes a user-custom opportunity.
     * Enforces creator ownership and flushes to assert foreign key constraints immediately.
     */
    @Transactional
    public void deleteOpportunity(Long id, Long requesterUserId) {
        if (id == null) {
            throw new IllegalArgumentException("Opportunity ID cannot be null");
        }

        Opportunity opportunity = opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + id));

        // Global system opportunities cannot be deleted by standard users
        if (opportunity.getCreatedByUser() == null) {
            throw new AccessDeniedException("Cannot delete global system opportunities");
        }

        // Only creator may delete
        if (requesterUserId == null || !Objects.equals(opportunity.getCreatedByUser().getId(), requesterUserId)) {
            throw new AccessDeniedException("Unauthorized: Only the creator may delete this custom opportunity");
        }

        opportunityRepository.delete(opportunity);
        opportunityRepository.flush();
    }
}
