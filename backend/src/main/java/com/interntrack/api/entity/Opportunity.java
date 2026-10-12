package com.interntrack.api.entity;

import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.OpportunityStatus;
import com.interntrack.api.entity.enums.SourceProvider;
import com.interntrack.api.entity.enums.StipendPeriod;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "opportunities",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_opp_provider_ext_id", columnNames = {"source_provider", "external_source_id"})
    },
    indexes = {
        @Index(name = "idx_opp_raw_hash", columnList = "raw_hash"),
        @Index(name = "idx_opp_status_deadline", columnList = "status, application_deadline")
    }
)
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_opp_company"))
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", foreignKey = @ForeignKey(name = "fk_opp_created_by_user"))
    private User createdByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_provider", nullable = false, length = 50)
    private SourceProvider sourceProvider = SourceProvider.SYSTEM;

    @Column(name = "external_source_id", length = 255)
    private String externalSourceId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 20)
    private LocationType locationType = LocationType.REMOTE;

    @Column(name = "location_city", length = 150)
    private String locationCity;

    @Column(name = "stipend_amount", precision = 12, scale = 2)
    private BigDecimal stipendAmount;

    @Column(name = "stipend_currency", length = 10)
    private String stipendCurrency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "stipend_period", length = 20)
    private StipendPeriod stipendPeriod = StipendPeriod.HOURLY;

    @Column(name = "duration_months")
    private Integer durationMonths;

    @Column(name = "application_deadline")
    private Instant applicationDeadline;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OpportunityStatus status = OpportunityStatus.ACTIVE;

    @Column(name = "raw_hash", length = 64)
    private String rawHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public Opportunity() {
    }

    public Opportunity(Company company, String title, LocationType locationType, SourceProvider sourceProvider) {
        this.company = company;
        this.title = title;
        this.locationType = locationType != null ? locationType : LocationType.REMOTE;
        this.sourceProvider = sourceProvider != null ? sourceProvider : SourceProvider.SYSTEM;
        this.status = OpportunityStatus.ACTIVE;
        this.stipendCurrency = "USD";
        this.stipendPeriod = StipendPeriod.HOURLY;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.status == null) {
            this.status = OpportunityStatus.ACTIVE;
        }
        if (this.locationType == null) {
            this.locationType = LocationType.REMOTE;
        }
        if (this.sourceProvider == null) {
            this.sourceProvider = SourceProvider.SYSTEM;
        }
        if (this.stipendCurrency == null) {
            this.stipendCurrency = "USD";
        }
        if (this.stipendPeriod == null) {
            this.stipendPeriod = StipendPeriod.HOURLY;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public SourceProvider getSourceProvider() {
        return sourceProvider;
    }

    public void setSourceProvider(SourceProvider sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    public String getExternalSourceId() {
        return externalSourceId;
    }

    public void setExternalSourceId(String externalSourceId) {
        this.externalSourceId = externalSourceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }

    public String getLocationCity() {
        return locationCity;
    }

    public void setLocationCity(String locationCity) {
        this.locationCity = locationCity;
    }

    public BigDecimal getStipendAmount() {
        return stipendAmount;
    }

    public void setStipendAmount(BigDecimal stipendAmount) {
        this.stipendAmount = stipendAmount;
    }

    public String getStipendCurrency() {
        return stipendCurrency;
    }

    public void setStipendCurrency(String stipendCurrency) {
        this.stipendCurrency = stipendCurrency;
    }

    public StipendPeriod getStipendPeriod() {
        return stipendPeriod;
    }

    public void setStipendPeriod(StipendPeriod stipendPeriod) {
        this.stipendPeriod = stipendPeriod;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    public Instant getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(Instant applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public OpportunityStatus getStatus() {
        return status;
    }

    public void setStatus(OpportunityStatus status) {
        this.status = status;
    }

    public String getRawHash() {
        return rawHash;
    }

    public void setRawHash(String rawHash) {
        this.rawHash = rawHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Opportunity that = (Opportunity) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Opportunity{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", sourceProvider=" + sourceProvider +
                ", locationType=" + locationType +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}