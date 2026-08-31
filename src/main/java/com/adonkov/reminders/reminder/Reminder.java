package com.adonkov.reminders.reminder;

import com.adonkov.reminders.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "reminders")
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(nullable = false)
    private boolean completed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Recurrence recurrence = Recurrence.NONE;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "notification_attempts", nullable = false)
    private int notificationAttempts;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "claimed_by", length = 100)
    private String claimedBy;

    /**
     * Guards against two concurrent edits of the same reminder silently
     * overwriting each other -- the second flush fails instead.
     */
    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Reminder() {
    }

    public Reminder(User user, String title, String description, Instant dueAt) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
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

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(Recurrence recurrence) {
        this.recurrence = recurrence;
    }

    public Instant getNotifiedAt() {
        return notifiedAt;
    }

    public int getNotificationAttempts() {
        return notificationAttempts;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public String getClaimedBy() {
        return claimedBy;
    }

    public long getVersion() {
        return version;
    }

    /**
     * Marks this reminder as delivered. A repeating reminder rolls forward to its
     * next occurrence and becomes pending again; a one-off stays notified.
     */
    public void markNotified(Instant when) {
        this.notificationAttempts++;
        this.claimedAt = null;
        this.claimedBy = null;

        Instant next = recurrence.nextAfter(dueAt);
        if (next != null) {
            this.dueAt = next;
            this.notifiedAt = null;
        } else {
            this.notifiedAt = when;
        }
    }

    public void markNotificationFailed() {
        this.notificationAttempts++;
        this.claimedAt = null;
        this.claimedBy = null;
    }

    public void snooze(java.time.Duration by) {
        this.dueAt = this.dueAt.plus(by);
        this.notifiedAt = null;
    }
}
