package com.interntrack.api.entity;

import com.interntrack.api.entity.enums.PreferredWorkMode;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "user_profiles")
public class UserProfile implements Persistable<Long> {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "id", foreignKey = @ForeignKey(name = "fk_user_profile_user"))
    private User user;

    @Column(name = "target_title", length = 100)
    private String targetTitle;

    @Column(name = "target_location", length = 100)
    private String targetLocation;

    @Column(name = "target_salary_min", precision = 12, scale = 2)
    private BigDecimal targetSalaryMin;

    @Column(name = "target_salary_max", precision = 12, scale = 2)
    private BigDecimal targetSalaryMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_work_mode", nullable = false, length = 20)
    private PreferredWorkMode preferredWorkMode = PreferredWorkMode.ANY;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    private boolean isNew = true;

    public UserProfile() {
    }

    public UserProfile(User user) {
        this.user = user;
        if (user != null) {
            this.id = user.getId();
        }
        this.preferredWorkMode = PreferredWorkMode.ANY;
        this.updatedAt = Instant.now();
        this.isNew = true;
    }

    public UserProfile(User user, String targetTitle, String targetLocation, BigDecimal targetSalaryMin, BigDecimal targetSalaryMax, PreferredWorkMode preferredWorkMode) {
        this.user = user;
        if (user != null) {
            this.id = user.getId();
        }
        this.targetTitle = targetTitle;
        this.targetLocation = targetLocation;
        this.targetSalaryMin = targetSalaryMin;
        this.targetSalaryMax = targetSalaryMax;
        this.preferredWorkMode = preferredWorkMode != null ? preferredWorkMode : PreferredWorkMode.ANY;
        this.updatedAt = Instant.now();
        this.isNew = true;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = Instant.now();
        if (this.preferredWorkMode == null) {
            this.preferredWorkMode = PreferredWorkMode.ANY;
        }
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.id = user.getId();
        }
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public void setTargetTitle(String targetTitle) {
        this.targetTitle = targetTitle;
    }

    public String getTargetLocation() {
        return targetLocation;
    }

    public void setTargetLocation(String targetLocation) {
        this.targetLocation = targetLocation;
    }

    public BigDecimal getTargetSalaryMin() {
        return targetSalaryMin;
    }

    public void setTargetSalaryMin(BigDecimal targetSalaryMin) {
        this.targetSalaryMin = targetSalaryMin;
    }

    public BigDecimal getTargetSalaryMax() {
        return targetSalaryMax;
    }

    public void setTargetSalaryMax(BigDecimal targetSalaryMax) {
        this.targetSalaryMax = targetSalaryMax;
    }

    public PreferredWorkMode getPreferredWorkMode() {
        return preferredWorkMode;
    }

    public void setPreferredWorkMode(PreferredWorkMode preferredWorkMode) {
        this.preferredWorkMode = preferredWorkMode != null ? preferredWorkMode : PreferredWorkMode.ANY;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfile that = (UserProfile) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "id=" + id +
                ", targetTitle='" + targetTitle + '\'' +
                ", targetLocation='" + targetLocation + '\'' +
                ", targetSalaryMin=" + targetSalaryMin +
                ", targetSalaryMax=" + targetSalaryMax +
                ", preferredWorkMode=" + preferredWorkMode +
                ", updatedAt=" + updatedAt +
                '}';
    }
}