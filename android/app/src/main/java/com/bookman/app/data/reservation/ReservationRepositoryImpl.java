package com.bookman.app.data.reservation;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;
import com.bookman.app.data.api.ApiCalls;
import com.bookman.app.data.api.CleaningStatusRequestDto;
import com.bookman.app.data.api.ImportResultDto;
import com.bookman.app.data.api.ReservationApi;
import com.bookman.app.data.api.ReservationDto;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class ReservationRepositoryImpl implements ReservationRepository {

    private final ReservationApi api;
    private final Executor io;

    public ReservationRepositoryImpl(ReservationApi api, Executor io) {
        this.api = api;
        this.io = io;
    }

    @Override
    public void list(long apartmentId, Consumer<Result<List<Reservation>>> callback) {
        io.execute(() -> {
            Result<List<ReservationDto>> result = ApiCalls.execute(api.list(apartmentId));
            if (!result.isSuccess()) {
                callback.accept(Result.failure(result.error()));
                return;
            }
            List<Reservation> items = new ArrayList<>();
            for (ReservationDto dto : result.value()) {
                items.add(toReservation(dto));
            }
            callback.accept(Result.success(items));
        });
    }

    @Override
    public void importNow(long apartmentId, Consumer<Result<Boolean>> callback) {
        io.execute(() -> {
            Result<ImportResultDto> result = ApiCalls.execute(api.importNow(apartmentId));
            callback.accept(result.isSuccess() ? Result.success(true) : Result.failure(result.error()));
        });
    }

    @Override
    public void changeCleaningStatus(long apartmentId, long reservationId, CleaningStatus status,
            Consumer<Result<Reservation>> callback) {
        io.execute(() -> {
            Result<ReservationDto> result = ApiCalls.execute(
                    api.changeCleaningStatus(apartmentId, reservationId, new CleaningStatusRequestDto(status.name())));
            callback.accept(result.isSuccess()
                    ? Result.success(toReservation(result.value()))
                    : Result.failure(result.error()));
        });
    }

    private static Reservation toReservation(ReservationDto dto) {
        return new Reservation(dto.id, LocalDate.parse(dto.checkIn), LocalDate.parse(dto.checkOut),
                CleaningStatus.valueOf(dto.cleaningStatus), dto.takenBy);
    }
}
