package com.bookman.reservation;

import com.bookman.apartment.Apartment;
import com.bookman.apartment.ApartmentRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * REQ-06 import of one apartment. Fail-safe (Q-08): if the source cannot be read completely the exception
 * propagates before anything is written, so a broken feed never cancels reservations.
 */
@Service
public class ReservationImporter {

    private final ApartmentRepository apartments;
    private final ReservationSource source;
    private final ReservationSync sync;
    private final Clock clock;

    public ReservationImporter(ApartmentRepository apartments, ReservationSource source, ReservationSync sync,
            Clock clock) {
        this.apartments = apartments;
        this.source = source;
        this.sync = sync;
        this.clock = clock;
    }

    public ImportResult importApartment(long apartmentId) throws ReservationSourceException {
        Optional<Apartment> apartment = apartments.findById(apartmentId);
        if (apartment.isEmpty()) {
            return ImportResult.skippedResult();
        }
        List<ImportedReservation> feed = source.fetch(apartment.get());
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(apartment.get().getTimeZone())));
        return sync.apply(apartmentId, feed, today);
    }
}
