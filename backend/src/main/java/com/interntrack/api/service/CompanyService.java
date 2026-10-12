package com.interntrack.api.service;

import com.interntrack.api.entity.Company;
import com.interntrack.api.exception.CompanyAlreadyExistsException;
import com.interntrack.api.repository.CompanyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    /**
     * Deterministically normalizes a company name into a canonical key.
     * Strips leading/trailing whitespace, converts to lowercase, and removes non-alphanumeric characters.
     */
    public String normalizeCanonicalName(String rawName) {
        if (rawName == null) {
            return null;
        }
        return rawName.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /**
     * Looks up a company by its primary key ID.
     */
    @Transactional(readOnly = true)
    public Optional<Company> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return companyRepository.findById(id);
    }

    /**
     * Looks up a company by its canonical name.
     */
    @Transactional(readOnly = true)
    public Optional<Company> findByCanonicalName(String canonicalName) {
        String normalized = normalizeCanonicalName(canonicalName);
        if (normalized == null || normalized.isBlank()) {
            return Optional.empty();
        }
        return companyRepository.findByCanonicalName(normalized);
    }

    /**
     * Checks if a company with the specified canonical name already exists.
     */
    @Transactional(readOnly = true)
    public boolean existsByCanonicalName(String canonicalName) {
        String normalized = normalizeCanonicalName(canonicalName);
        if (normalized == null || normalized.isBlank()) {
            return false;
        }
        return companyRepository.existsByCanonicalName(normalized);
    }

    /**
     * Retrieves all companies.
     */
    @Transactional(readOnly = true)
    public List<Company> findAll() {
        return companyRepository.findAll();
    }

    /**
     * Searches companies matching a keyword query against company name.
     * If query is null or blank, all companies are returned.
     */
    @Transactional(readOnly = true)
    public List<Company> searchByName(String query) {
        if (query == null || query.isBlank()) {
            return companyRepository.findAll();
        }
        return companyRepository.findByNameContainingIgnoreCase(query.trim());
    }

    /**
     * Creates and persists a new Company record.
     *
     * @param name                 official employer name
     * @param domain               optional web domain (e.g. "stripe.com")
     * @param industry             optional industry sector
     * @param logoUrl              optional branding logo URL
     * @param headquartersLocation optional city/state location
     * @return persisted Company entity
     * @throws CompanyAlreadyExistsException if a company with the same canonical name exists
     * @throws IllegalArgumentException     if company name is null, blank, or canonical name resolves to empty
     */
    @Transactional
    public Company createCompany(String name, String domain, String industry, String logoUrl, String headquartersLocation) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Company name cannot be null or blank");
        }

        String canonicalName = normalizeCanonicalName(name);
        if (canonicalName == null || canonicalName.isBlank()) {
            throw new IllegalArgumentException("Company name must contain alphanumeric characters");
        }

        if (companyRepository.existsByCanonicalName(canonicalName)) {
            throw new CompanyAlreadyExistsException("Company with canonical name '" + canonicalName + "' already exists");
        }

        Company company = new Company();
        company.setName(name.trim());
        company.setCanonicalName(canonicalName);
        company.setDomain(domain != null && !domain.isBlank() ? domain.trim().toLowerCase() : null);
        company.setIndustry(industry != null && !industry.isBlank() ? industry.trim() : null);
        company.setLogoUrl(logoUrl != null && !logoUrl.isBlank() ? logoUrl.trim() : null);
        company.setHeadquartersLocation(headquartersLocation != null && !headquartersLocation.isBlank() ? headquartersLocation.trim() : null);

        try {
            return companyRepository.saveAndFlush(company);
        } catch (DataIntegrityViolationException ex) {
            String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
            if (rootMsg != null && (rootMsg.contains("uk_company_canonical") || rootMsg.toLowerCase().contains("canonical_name"))) {
                throw new CompanyAlreadyExistsException("Company with canonical name '" + canonicalName + "' already exists");
            }
            throw ex;
        }
    }
}