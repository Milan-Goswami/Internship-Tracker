package com.interntrack.api.entity;

import com.interntrack.api.entity.enums.InterviewOutcome;
import com.interntrack.api.entity.enums.InterviewRoundType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "interviews",
    indexes = {
        @Index(name = "idx_interview_app_scheduled", columnList = "application_id, scheduled_at")
    }
)
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, foreignKey = @ForeignKey(name = "fk_interview_application"))
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Application application;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "round_title", nullable = false, length = 150)
    private String roundTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "round_type", nullable = false, length = 30)
    private InterviewRoundType roundType;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 60;

    @Column(name = "interviewer_names", length = 255)
    private String interviewerNames;

    @Column(name = "interviewer_emails", length = 255)
    private String interviewerEmails;

    @Column(name = "prep_notes", columnDefinition = "TEXT")
    private String prepNotes;

    @Column(name = "reflection_notes", columnDefinition = "TEXT")
    private String reflectionNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private InterviewOutcome outcome = InterviewOutcome.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Interview() {
    }

    public Interview(Application application, Integer roundNumber, String roundTitle,
                     InterviewRoundType roundType, Instant scheduledAt) {
        this.application = application;
        this.roundNumber = roundNumber;
        this.roundTitle = roundTitle;
        this.roundType = roundType;
        this.scheduledAt = scheduledAt;
        this.durationMinutes = 60;
        this.outcome = InterviewOutcome.PENDING;
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
        if (this.durationMinutes == null) {
            this.durationMinutes = 60;
        }
        if (this.outcome == null) {
            this.outcome = InterviewOutcome.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getRoundTitle() {
        return roundTitle;
    }

    public void setRoundTitle(String roundTitle) {
        this.roundTitle = roundTitle;
    }

    public InterviewRoundType getRoundType() {
        return roundType;
    }

    public void setRoundType(InterviewRoundType roundType) {
        this.roundType = roundType;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getInterviewerNames() {
        return interviewerNames;
    }

    public void setInterviewerNames(String interviewerNames) {
        this.interviewerNames = interviewerNames;
    }

    public String getInterviewerEmails() {
        return interviewerEmails;
    }

    public void setInterviewerEmails(String interviewerEmails) {
        this.interviewerEmails = interviewerEmails;
    }

    public String getPrepNotes() {
        return prepNotes;
    }

    public void setPrepNotes(String prepNotes) {
        this.prepNotes = prepNotes;
    }

    public String getReflectionNotes() {
        return reflectionNotes;
    }

    public void setReflectionNotes(String reflectionNotes) {
        this.reflectionNotes = reflectionNotes;
    }

    public InterviewOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(InterviewOutcome outcome) {
        this.outcome = outcome;
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
        Interview that = (Interview) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Interview{" +
                "id=" + id +
                ", roundNumber=" + roundNumber +
                ", roundTitle='" + roundTitle + '\'' +
                ", roundType=" + roundType +
                ", scheduledAt=" + scheduledAt +
                ", durationMinutes=" + durationMinutes +
                ", outcome=" + outcome +
                '}';
    }
}