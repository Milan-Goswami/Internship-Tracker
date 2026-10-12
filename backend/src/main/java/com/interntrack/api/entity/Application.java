package com.interntrack.api.entity;

import com.interntrack.api.entity.enums.ApplicationStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "applications",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_app_user_opp", columnNames = {"user_id", "opportunity_id"})
    },
    indexes = {
        @Index(name = "idx_app_user_status", columnList = "user_id, status"),
        @Index(name = "idx_app_user_updated", columnList = "user_id, updated_at")
    }
)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_app_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opportunity_id", nullable = false, foreignKey = @ForeignKey(name = "fk_app_opportunity"))
    private Opportunity opportunity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.SAVED;

    @Column(name = "resume_document_id")
    private Long resumeDocumentId;

    @Column(name = "cover_letter_document_id")
    private Long coverLetterDocumentId;

    @Column(name = "personal_notes", columnDefinition = "TEXT")
    private String personalNotes;

    @Column(name = "user_rating")
    private Integer userRating;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "screening_at")
    private Instant screeningAt;

    @Column(name = "interviewing_at")
    private Instant interviewingAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Application() {
    }

    public Application(User user, Opportunity opportunity, ApplicationStatus status) {
        this.user = user;
        this.opportunity = opportunity;
        this.status = status != null ? status : ApplicationStatus.SAVED;
        this.savedAt = Instant.now();
        this.updatedAt = this.savedAt;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.savedAt == null) {
            this.savedAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.status == null) {
            this.status = ApplicationStatus.SAVED;
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

    public Opportunity getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(Opportunity opportunity) {
        this.opportunity = opportunity;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public Long getResumeDocumentId() {
        return resumeDocumentId;
    }

    public void setResumeDocumentId(Long resumeDocumentId) {
        this.resumeDocumentId = resumeDocumentId;
    }

    public Long getCoverLetterDocumentId() {
        return coverLetterDocumentId;
    }

    public void setCoverLetterDocumentId(Long coverLetterDocumentId) {
        this.coverLetterDocumentId = coverLetterDocumentId;
    }

    public String getPersonalNotes() {
        return personalNotes;
    }

    public void setPersonalNotes(String personalNotes) {
        this.personalNotes = personalNotes;
    }

    public Integer getUserRating() {
        return userRating;
    }

    public void setUserRating(Integer userRating) {
        this.userRating = userRating;
    }

    public Instant getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(Instant savedAt) {
        this.savedAt = savedAt;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    public Instant getScreeningAt() {
        return screeningAt;
    }

    public void setScreeningAt(Instant screeningAt) {
        this.screeningAt = screeningAt;
    }

    public Instant getInterviewingAt() {
        return interviewingAt;
    }

    public void setInterviewingAt(Instant interviewingAt) {
        this.interviewingAt = interviewingAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Application that = (Application) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Application{" +
                "id=" + id +
                ", status=" + status +
                ", savedAt=" + savedAt +
                ", appliedAt=" + appliedAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}