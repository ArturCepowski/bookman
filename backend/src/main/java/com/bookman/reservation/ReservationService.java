package com.bookman.reservation;

import com.bookman.apartment.ApartmentAccess;
import com.bookman.apartment.ApartmentRepository;
import com.bookman.apartment.Role;
import com.bookman.common.ApiException;
import com.bookman.common.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private final ApartmentAccess access;
    private final ApartmentRepository apartments;
    private final ReservationRepository reservations;
    private final ReservationStatusChangeRepository history;
    private final ReservationImporter importer;
    private final Clock clock;

    public ReservationService(ApartmentAccess access, ApartmentRepository apartments,
            ReservationRepository reservations, ReservationStatusChangeRepository history,
            ReservationImporter importer, Clock clock) {
        this.access = access;
        this.apartments = apartments;
        this.reservations = reservations;
        this.history = history;
        this.importer = importer;
        this.clock = clock;
    }

    /** REQ-07: visible to ADMIN and CLEANING_SERVICE of the apartment. */
    @Transactional(readOnly = true)
    public List<ReservationView> listCurrent(long apartmentId, long userId) {
        access.requireMember(apartmentId, userId);
        ZoneId zone = ZoneId.of(apartments.getReferenceById(apartmentId).getTimeZone());
        return reservations.findCurrent(apartmentId, LocalDate.now(clock.withZone(zone)));
    }

    /** REQ-06: manual import (pull to refresh) by any member of the apartment. */
    public ImportResult importNow(long apartmentId, long userId) {
        access.requireMember(apartmentId, userId);
        try {
            return importer.importApartment(apartmentId);
        } catch (ReservationSourceException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, ErrorCode.IMPORT_FAILED,
                    "Reservations could not be refreshed from Booking.com");
        }
    }

    /** REQ-08 */
    @Transactional
    public ReservationView changeCleaningStatus(long apartmentId, long reservationId, long userId,
            CleaningStatus requested) {
        Role role = access.requireMember(apartmentId, userId);
        Reservation reservation = reservations.findByIdAndApartmentId(reservationId, apartmentId)
                .filter(r -> !r.isCancelled())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "No such reservation"));
        CleaningStatus current = reservation.getCleaningStatus();
        CleaningStatusPolicy.check(role, current, requested);

        reservation.changeCleaningStatus(requested, userId);
        reservations.saveAndFlush(reservation);
        history.save(new ReservationStatusChange(reservationId, current, requested, userId, clock.instant()));
        return reservations.findView(reservationId);
    }
}
