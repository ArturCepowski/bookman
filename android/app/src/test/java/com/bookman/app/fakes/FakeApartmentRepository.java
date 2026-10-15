package com.bookman.app.fakes;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;
import com.bookman.app.data.apartment.ApartmentDetails;
import com.bookman.app.data.apartment.ApartmentInput;
import com.bookman.app.data.apartment.ApartmentRepository;
import com.bookman.app.data.apartment.ApartmentSummary;
import com.bookman.app.data.apartment.Role;
import java.util.ArrayList;
import java.util.List;

public class FakeApartmentRepository implements ApartmentRepository {

    public Result<List<ApartmentSummary>> listResult = Result.success(new ArrayList<>());
    public Result<ApartmentDetails> detailsResult = Result.success(details(Role.ADMIN));
    public Result<Boolean> deleteResult = Result.success(true);
    public ApartmentInput lastInput;
    public Long lastUpdatedId;
    public Long lastDeletedId;
    public int creates;

    public static ApartmentDetails details(Role role) {
        return new ApartmentDetails(5, "Sunny flat", "https://www.booking.com/hotel/pl/sunny.html",
                role == Role.ADMIN ? "https://ical.booking.com/v1/export?t=abc" : null, "Europe/Warsaw", role);
    }

    @Override
    public void list(Consumer<Result<List<ApartmentSummary>>> callback) {
        callback.accept(listResult);
    }

    @Override
    public void get(long id, Consumer<Result<ApartmentDetails>> callback) {
        callback.accept(detailsResult);
    }

    @Override
    public void create(ApartmentInput input, Consumer<Result<ApartmentDetails>> callback) {
        creates++;
        lastInput = input;
        callback.accept(detailsResult);
    }

    @Override
    public void update(long id, ApartmentInput input, Consumer<Result<ApartmentDetails>> callback) {
        lastUpdatedId = id;
        lastInput = input;
        callback.accept(detailsResult);
    }

    @Override
    public void delete(long id, Consumer<Result<Boolean>> callback) {
        lastDeletedId = id;
        callback.accept(deleteResult);
    }
}
