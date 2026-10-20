package com.bookman.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** REQ-08 audit trail: who changed the cleaning status and when. */
@Entity
@Table(name = "reservation_status_change")
public class ReservationStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false)
    private CleaningStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private CleaningStatus toStatus;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected ReservationStatusChange() {
    }

    public ReservationStatusChange(long reservationId, CleaningStatus from, CleaningStatus to, long changedBy,
            Instant changedAt) {
        this.reservationId = reservationId;
        this.fromStatus = from;
        this.toStatus = to;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    public CleaningStatus getFromStatus() {
        return fromStatus;
    }

    public CleaningStatus getToStatus() {
        return toStatus;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
