package com.bookman.device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** FCM registration token of one phone (Q-24). */
@Entity
@Table(name = "device_token")
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private String token;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    protected DeviceToken() {
    }

    DeviceToken(long userId, String token, Instant now) {
        this.userId = userId;
        this.token = token;
        this.createdAt = now;
        this.lastSeenAt = now;
    }

    /** The same phone, now logged in as {@code userId}. */
    void assignTo(long userId, Instant now) {
        this.userId = userId;
        this.lastSeenAt = now;
    }

    public Long getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }
}
