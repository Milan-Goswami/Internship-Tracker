package com.interntrack.api.entity;

import com.interntrack.api.entity.enums.ProficiencyLevel;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "user_skills",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_skill", columnNames = {"user_id", "skill_id"})
    },
    indexes = {
        @Index(name = "idx_user_skill_user", columnList = "user_id"),
        @Index(name = "idx_user_skill_skill", columnList = "skill_id")
    }
)
public class UserSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_skill_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_skill_skill"))
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "proficiency_level", nullable = false, length = 20)
    private ProficiencyLevel proficiencyLevel = ProficiencyLevel.INTERMEDIATE;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserSkill() {
    }

    public UserSkill(User user, Skill skill, ProficiencyLevel proficiencyLevel, Integer yearsOfExperience) {
        this.user = user;
        this.skill = skill;
        this.proficiencyLevel = proficiencyLevel != null ? proficiencyLevel : ProficiencyLevel.INTERMEDIATE;
        this.yearsOfExperience = yearsOfExperience;
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
        if (this.proficiencyLevel == null) {
            this.proficiencyLevel = ProficiencyLevel.INTERMEDIATE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public ProficiencyLevel getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(ProficiencyLevel proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSkill userSkill = (UserSkill) o;
        return id != null && Objects.equals(id, userSkill.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "UserSkill{" +
                "id=" + id +
                ", proficiencyLevel=" + proficiencyLevel +
                ", yearsOfExperience=" + yearsOfExperience +
                ", createdAt=" + createdAt +
                '}';
    }
}