package com.bookman.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One reminder for one recipient (sent to all their phones); created by {@link ReminderPlanner}, sent by
 * {@link ReminderDispatcher}.
 */
@Entity
@Table(name = "reminder_notification")
public class ReminderNotification {

    /** REQ-13 (now push, Q-24): failures are retried at most 3 times (4 attempts), backoff 2 s / 4 s / 8 s (Q-10). */
    static final int MAX_ATTEMPTS = 4;

    public enum Status {
        PENDING,
        SENT,
        FAILED,
        /** The recipient has no phone with the app registered (or all tokens were invalid). */
        NO_DEVICE
    }

    @Id
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false)
    private ReminderType reminderType;

    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected ReminderNotification() {
    }

    void markNoDevice(Instant now) {
        attempts++;
        status = Status.NO_DEVICE;
        lastError = null;
        nextAttemptAt = now;
    }

    void markSent(Instant now) {
        attempts++;
        status = Status.SENT;
        sentAt = now;
        lastError = null;
    }

    void markFailed(String error, Instant now) {
        attempts++;
        lastError = error == null ? null : error.substring(0, Math.min(error.length(), 500));
        if (attempts >= MAX_ATTEMPTS) {
            status = Status.FAILED;
        } else {
            nextAttemptAt = now.plus(Duration.ofSeconds(1L << attempts));
        }
    }

    public Long getId() {
        return id;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public ReminderType getReminderType() {
        return reminderType;
    }

    public LocalDate getReminderDate() {
        return reminderDate;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Status getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }
}
