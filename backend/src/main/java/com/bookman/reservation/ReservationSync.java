package com.bookman.reservation;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Applies a complete feed to the stored reservations of one apartment, atomically. */
@Component
public class ReservationSync {

    private final ReservationRepository reservations;
    private final Clock clock;

    public ReservationSync(ReservationRepository reservations, Clock clock) {
        this.reservations = reservations;
        this.clock = clock;
    }

    // REQUIRES_NEW: also called right after the apartment registration commits (event listener).
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportResult apply(long apartmentId, List<ImportedReservation> feed, LocalDate today) {
        if (!reservations.tryLockForImport(apartmentId)) {
            return ImportResult.skippedResult();
        }
        Map<String, ImportedReservation> incoming = new LinkedHashMap<>();
        feed.forEach(r -> incoming.put(r.externalId(), r));
        Map<String, Reservation> stored = reservations.findByApartmentId(apartmentId).stream()
                .collect(Collectors.toMap(Reservation::getExternalId, Function.identity()));

        int added = 0;
        int updated = 0;
        int cancelled = 0;
        for (ImportedReservation imported : incoming.values()) {
            Reservation existing = stored.get(imported.externalId());
            if (existing == null) {
                reservations.save(new Reservation(apartmentId, imported, clock.instant()));
                added++;
            } else if (existing.refreshFrom(imported)) {
                updated++;
            }
        }
        for (Reservation existing : stored.values()) {
            boolean gone = !incoming.containsKey(existing.getExternalId());
            // REQ-06: only stays that have not ended yet are cancelled; past ones are left as they are.
            if (gone && !existing.isCancelled() && !existing.getCheckOut().isBefore(today)) {
                existing.cancel(clock.instant());
                cancelled++;
            }
        }
        return new ImportResult(added, updated, cancelled, false);
    }
}
