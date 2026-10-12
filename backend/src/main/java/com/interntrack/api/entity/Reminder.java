package com.interntrack.api.entity;

import com.interntrack.api.entity.enums.ReminderType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "reminders",
    indexes = {
        @Index(name = "idx_reminder_user_due", columnList = "user_id, completed, due_at"),
        @Index(name = "idx_reminder_user_type_comp", columnList = "user_id, completed, reminder_type")
    }
)
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reminder_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", foreignKey = @ForeignKey(name = "fk_reminder_application"))
    private Application application;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 30)
    private ReminderType reminderType = ReminderType.CUSTOM;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Reminder() {
    }

    public Reminder(User user, Application application, String title, Instant dueAt, ReminderType reminderType) {
        this.user = user;
        this.application = application;
        this.title = title;
        this.dueAt = dueAt;
        this.reminderType = reminderType != null ? reminderType : ReminderType.CUSTOM;
        this.completed = false;
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
        if (this.completed == null) {
            this.completed = false;
        }
        if (this.reminderType == null) {
            this.reminderType = ReminderType.CUSTOM;
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

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public ReminderType getReminderType() {
        return reminderType;
    }

    public void setReminderType(ReminderType reminderType) {
        this.reminderType = reminderType;
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
        Reminder reminder = (Reminder) o;
        return id != null && Objects.equals(id, reminder.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Reminder{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", dueAt=" + dueAt +
                ", completed=" + completed +
                ", reminderType=" + reminderType +
                '}';
    }
}