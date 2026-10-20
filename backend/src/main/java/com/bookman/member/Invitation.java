package com.bookman.member;

import com.bookman.apartment.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "invitation")
public class Invitation {

    /** REQ-09 */
    static final Duration VALIDITY = Duration.ofDays(14);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "apartment_id", nullable = false, updatable = false)
    private Long apartmentId;

    @Column(nullable = false, updatable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status;

    @Column(name = "invited_by")
    private Long invitedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected Invitation() {
    }

    Invitation(long apartmentId, String normalizedEmail, long invitedBy, Instant now) {
        this.apartmentId = apartmentId;
        this.email = normalizedEmail;
        this.role = Role.CLEANING_SERVICE;
        this.status = InvitationStatus.PENDING;
        this.invitedBy = invitedBy;
        this.createdAt = now;
        this.expiresAt = now.plus(VALIDITY);
    }

    /** Pending and not yet expired. */
    boolean isOpen(Instant now) {
        return status == InvitationStatus.PENDING && now.isBefore(expiresAt);
    }

    boolean isExpired(Instant now) {
        return status == InvitationStatus.PENDING && !now.isBefore(expiresAt);
    }

    void close(InvitationStatus outcome, Instant now) {
        this.status = outcome;
        this.respondedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getApartmentId() {
        return apartmentId;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public Long getInvitedBy() {
        return invitedBy;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
