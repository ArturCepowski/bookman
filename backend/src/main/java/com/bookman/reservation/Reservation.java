package com.bookman.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "apartment_id", nullable = false, updatable = false)
    private Long apartmentId;

    @Column(name = "external_id", nullable = false, updatable = false)
    private String externalId;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Enumerated(EnumType.STRING)
    @Column(name = "cleaning_status", nullable = false)
    private CleaningStatus cleaningStatus;

    @Column(name = "responsible_user_id")
    private Long responsibleUserId;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Two people changing the same reservation at once: the second write fails instead of silently winning.
    @Version
    private long version;

    protected Reservation() {
    }

    public Reservation(long apartmentId, ImportedReservation imported, Instant createdAt) {
        this.apartmentId = apartmentId;
        this.externalId = imported.externalId();
        this.checkIn = imported.checkIn();
        this.checkOut = imported.checkOut();
        this.cleaningStatus = CleaningStatus.NOT_PLANNED;
        this.createdAt = createdAt;
    }

    /** @return true if anything changed */
    boolean refreshFrom(ImportedReservation imported) {
        boolean changed = cancelledAt != null
                || !checkIn.equals(imported.checkIn())
                || !checkOut.equals(imported.checkOut());
        checkIn = imported.checkIn();
        checkOut = imported.checkOut();
        // ASSUMPTION(REQ-06): a cancelled reservation that reappears in the feed is active again (Q-20).
        cancelledAt = null;
        return changed;
    }

    void cancel(Instant when) {
        cancelledAt = when;
    }

    // Q-21: whoever sets TAKEN (admin included) is responsible; DONE keeps it; NOT_PLANNED/PRIVATE clear it.
    void changeCleaningStatus(CleaningStatus newStatus, long actorId) {
        if (newStatus == CleaningStatus.TAKEN) {
            responsibleUserId = actorId;
        } else if (newStatus == CleaningStatus.NOT_PLANNED || newStatus == CleaningStatus.PRIVATE) {
            responsibleUserId = null;
        }
        cleaningStatus = newStatus;
    }

    public Long getId() {
        return id;
    }

    public Long getApartmentId() {
        return apartmentId;
    }

    public String getExternalId() {
        return externalId;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public CleaningStatus getCleaningStatus() {
        return cleaningStatus;
    }

    public Long getResponsibleUserId() {
        return responsibleUserId;
    }

    public boolean isCancelled() {
        return cancelledAt != null;
    }
}
