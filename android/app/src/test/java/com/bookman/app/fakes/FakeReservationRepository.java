package com.bookman.app.fakes;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;
import com.bookman.app.data.reservation.CleaningStatus;
import com.bookman.app.data.reservation.Reservation;
import com.bookman.app.data.reservation.ReservationRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FakeReservationRepository implements ReservationRepository {

    public Result<List<Reservation>> listResult = Result.success(new ArrayList<>());
    public Result<Boolean> importResult = Result.success(true);
    public Result<Reservation> changeResult;
    public final List<String> calls = new ArrayList<>();
    public CleaningStatus lastRequestedStatus;

    public static Reservation reservation(long id, CleaningStatus status, String takenBy) {
        return new Reservation(id, LocalDate.of(2026, 10, 22), LocalDate.of(2026, 10, 25), status, takenBy);
    }

    @Override
    public void list(long apartmentId, Consumer<Result<List<Reservation>>> callback) {
        calls.add("list");
        callback.accept(listResult);
    }

    @Override
    public void importNow(long apartmentId, Consumer<Result<Boolean>> callback) {
        calls.add("import");
        callback.accept(importResult);
    }

    @Override
    public void changeCleaningStatus(long apartmentId, long reservationId, CleaningStatus status,
            Consumer<Result<Reservation>> callback) {
        calls.add("change");
        lastRequestedStatus = status;
        callback.accept(changeResult);
    }
}
