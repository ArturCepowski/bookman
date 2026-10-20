package com.bookman.reservation;

import java.time.LocalDate;

/** A stay as reported by a {@link ReservationSource}. */
public record ImportedReservation(String externalId, LocalDate checkIn, LocalDate checkOut) {
}
