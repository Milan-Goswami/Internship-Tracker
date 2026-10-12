package com.interntrack.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "opportunity_skills",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_opp_skill", columnNames = {"opportunity_id", "skill_id"})
    },
    indexes = {
        @Index(name = "idx_opp_skill_opp", columnList = "opportunity_id"),
        @Index(name = "idx_opp_skill_skill", columnList = "skill_id"),
        @Index(name = "idx_opp_skill_required", columnList = "opportunity_id, is_required")
    }
)
public class OpportunitySkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opportunity_id", nullable = false, foreignKey = @ForeignKey(name = "fk_opp_skill_opportunity"))
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false, foreignKey = @ForeignKey(name = "fk_opp_skill_skill"))
    private Skill skill;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public OpportunitySkill() {
    }

    public OpportunitySkill(Opportunity opportunity, Skill skill, Boolean isRequired) {
        this.opportunity = opportunity;
        this.skill = skill;
        this.isRequired = isRequired != null ? isRequired : true;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.isRequired == null) {
            this.isRequired = true;
        }
    }

    public Long getId() {
        return id;
    }

    public Opportunity getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(Opportunity opportunity) {
        this.opportunity = opportunity;
    }

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean required) {
        isRequired = required;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpportunitySkill that = (OpportunitySkill) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "OpportunitySkill{" +
                "id=" + id +
                ", isRequired=" + isRequired +
                ", createdAt=" + createdAt +
                '}';
    }
}