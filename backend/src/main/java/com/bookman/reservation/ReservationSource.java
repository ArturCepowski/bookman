package com.bookman.reservation;

import com.bookman.apartment.Apartment;
import java.util.List;

/**
 * REQ-06: where reservations come from. v1 reads the Booking.com iCal export; another method
 * (e.g. the Connectivity API, other channels) can be added by implementing this interface.
 */
public interface ReservationSource {

    /** Returns the complete current list of stays; throws if it cannot be read fully (nothing is changed then). */
    List<ImportedReservation> fetch(Apartment apartment) throws ReservationSourceException;
}
