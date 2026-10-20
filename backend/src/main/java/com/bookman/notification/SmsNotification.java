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

/** One reminder SMS to one recipient; rows are created by {@link ReminderPlanner} and sent by {@link SmsDispatcher}. */
@Entity
@Table(name = "sms_notification")
public class SmsNotification {

    /** REQ-13: failed sends are retried at most 3 times (4 attempts in total), backoff 2 s / 4 s / 8 s (Q-10). */
    static final int MAX_ATTEMPTS = 4;

    public enum Status {
        PENDING,
        SENT,
        FAILED
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

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String message;

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

    protected SmsNotification() {
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getMessage() {
        return message;
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
