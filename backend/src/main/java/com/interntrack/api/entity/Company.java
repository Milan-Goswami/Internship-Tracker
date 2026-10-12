package com.interntrack.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "companies",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_company_canonical", columnNames = {"canonical_name"})
    }
)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "canonical_name", nullable = false, length = 150)
    private String canonicalName;

    @Column(name = "domain", length = 100)
    private String domain;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "headquarters_location", length = 150)
    private String headquartersLocation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Company() {
    }

    public Company(String name, String domain, String industry) {
        this.name = name;
        this.canonicalName = normalizeCanonicalName(name);
        this.domain = domain;
        this.industry = industry;
    }

    public static String normalizeCanonicalName(String rawName) {
        if (rawName == null) {
            return null;
        }
        return rawName.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.canonicalName == null && this.name != null) {
            this.canonicalName = normalizeCanonicalName(this.name);
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if (this.canonicalName == null && name != null) {
            this.canonicalName = normalizeCanonicalName(name);
        }
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getHeadquartersLocation() {
        return headquartersLocation;
    }

    public void setHeadquartersLocation(String headquartersLocation) {
        this.headquartersLocation = headquartersLocation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Company company = (Company) o;
        return id != null && Objects.equals(id, company.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Company{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", canonicalName='" + canonicalName + '\'' +
                ", domain='" + domain + '\'' +
                ", industry='" + industry + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}