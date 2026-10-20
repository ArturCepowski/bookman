package com.bookman.app.data.reservation;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;
import java.util.List;

public interface ReservationRepository {

    /** REQ-07: current and future reservations, sorted by check-out. */
    void list(long apartmentId, Consumer<Result<List<Reservation>>> callback);

    /** REQ-06: asks the backend to import from Booking.com now (pull to refresh). */
    void importNow(long apartmentId, Consumer<Result<Boolean>> callback);

    /** REQ-08 */
    void changeCleaningStatus(long apartmentId, long reservationId, CleaningStatus status,
            Consumer<Result<Reservation>> callback);
}
