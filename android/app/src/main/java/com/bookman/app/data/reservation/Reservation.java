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

    public Reservation(long id, LocalDate checkIn, LocalDate checkOut, CleaningStatus cleaningStatus,
            @Nullable String takenBy) {
        this.id = id;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.cleaningStatus = cleaningStatus;
        this.takenBy = takenBy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Reservation)) {
            return false;
        }
        Reservation other = (Reservation) o;
        return id == other.id && checkIn.equals(other.checkIn) && checkOut.equals(other.checkOut)
                && cleaningStatus == other.cleaningStatus && Objects.equals(takenBy, other.takenBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, checkIn, checkOut, cleaningStatus, takenBy);
    }
}
