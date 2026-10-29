package com.bookman.reservation;

import java.time.LocalDate;

/**
 * REQ-07 row. {@code takenBy} is the e-mail of the user who took the cleaning (TAKEN/DONE only), else null.
 * {@code changedByMe}: the current status was set by the user asking (Q-28: the cleaning service may undo it).
 */
public record ReservationView(Long id, LocalDate checkIn, LocalDate checkOut, CleaningStatus cleaningStatus,
        String takenBy, boolean changedByMe) {
}
