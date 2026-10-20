package com.bookman.reservation;

import java.time.LocalDate;

/** REQ-07 row. {@code takenBy} is the e-mail of the user who took the cleaning (TAKEN/DONE only), else null. */
public record ReservationView(Long id, LocalDate checkIn, LocalDate checkOut, CleaningStatus cleaningStatus,
        String takenBy) {
}
