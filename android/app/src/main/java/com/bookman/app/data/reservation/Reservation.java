package com.bookman.app.data.reservation;

import androidx.annotation.Nullable;
import java.time.LocalDate;
import java.util.Objects;

public final class Reservation {

    public final long id;
    public final LocalDate checkIn;
    public final LocalDate checkOut;
    public final CleaningStatus cleaningStatus;
    /** E-mail of the person who took the cleaning (REQ-07), only for TAKEN/DONE. */
    @Nullable public final String takenBy;
    /** Q-28: the current status was set by the logged-in user, who may then undo it. */
    public final boolean changedByMe;

    public Reservation(long id, LocalDate checkIn, LocalDate checkOut, CleaningStatus cleaningStatus,
            @Nullable String takenBy, boolean changedByMe) {
        this.id = id;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.cleaningStatus = cleaningStatus;
        this.takenBy = takenBy;
        this.changedByMe = changedByMe;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Reservation)) {
            return false;
        }
        Reservation other = (Reservation) o;
        return id == other.id && checkIn.equals(other.checkIn) && checkOut.equals(other.checkOut)
                && cleaningStatus == other.cleaningStatus && Objects.equals(takenBy, other.takenBy)
                && changedByMe == other.changedByMe;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, checkIn, checkOut, cleaningStatus, takenBy, changedByMe);
    }
}
