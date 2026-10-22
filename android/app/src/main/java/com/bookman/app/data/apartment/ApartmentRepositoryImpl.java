package com.bookman.app.data.apartment;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;
import com.bookman.app.data.api.ApartmentApi;
import com.bookman.app.data.api.ApartmentDto;
import com.bookman.app.data.api.ApartmentRequestDto;
import com.bookman.app.data.api.ApiCalls;
import com.bookman.app.data.api.NotificationSettingsDto;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import retrofit2.Call;

public class ApartmentRepositoryImpl implements ApartmentRepository {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final ApartmentApi api;
    private final Executor io;

    public ApartmentRepositoryImpl(ApartmentApi api, Executor io) {
        this.api = api;
        this.io = io;
    }

    @Override
    public void list(Consumer<Result<List<ApartmentSummary>>> callback) {
        io.execute(() -> {
            Result<List<ApartmentDto>> result = ApiCalls.execute(api.list());
            if (!result.isSuccess()) {
                callback.accept(Result.failure(result.error()));
                return;
            }
            List<ApartmentSummary> items = new ArrayList<>();
            for (ApartmentDto dto : result.value()) {
                items.add(new ApartmentSummary(dto.id, dto.name, Role.valueOf(dto.role)));
            }
            callback.accept(Result.success(items));
        });
    }

    @Override
    public void get(long id, Consumer<Result<ApartmentDetails>> callback) {
        details(api.get(id), callback);
    }

    @Override
    public void create(ApartmentInput input, Consumer<Result<ApartmentDetails>> callback) {
        details(api.create(toDto(input)), callback);
    }

    @Override
    public void update(long id, ApartmentInput input, Consumer<Result<ApartmentDetails>> callback) {
        details(api.update(id, toDto(input)), callback);
    }

    @Override
    public void delete(long id, Consumer<Result<Boolean>> callback) {
        io.execute(() -> callback.accept(ApiCalls.executeNoContent(api.delete(id))));
    }

    @Override
    public void notificationSettings(long id, Consumer<Result<NotificationSettings>> callback) {
        settings(api.notificationSettings(id), callback);
    }

    @Override
    public void updateNotificationSettings(long id, NotificationSettings settings,
            Consumer<Result<NotificationSettings>> callback) {
        settings(api.updateNotificationSettings(id,
                new NotificationSettingsDto(format(settings.notPlannedTime), format(settings.notDoneTime))), callback);
    }

    private void settings(Call<NotificationSettingsDto> call, Consumer<Result<NotificationSettings>> callback) {
        io.execute(() -> {
            Result<NotificationSettingsDto> result = ApiCalls.execute(call);
            callback.accept(result.isSuccess()
                    ? Result.success(new NotificationSettings(parse(result.value().notPlannedTime),
                            parse(result.value().notDoneTime)))
                    : Result.failure(result.error()));
        });
    }

    private static LocalTime parse(String value) {
        return value == null ? null : LocalTime.parse(value, HH_MM);
    }

    private static String format(LocalTime value) {
        return value == null ? null : HH_MM.format(value);
    }

    private void details(Call<ApartmentDto> call, Consumer<Result<ApartmentDetails>> callback) {
        io.execute(() -> {
            Result<ApartmentDto> result = ApiCalls.execute(call);
            if (!result.isSuccess()) {
                callback.accept(Result.failure(result.error()));
                return;
            }
            ApartmentDto dto = result.value();
            callback.accept(Result.success(new ApartmentDetails(dto.id, dto.name, dto.bookingUrl, dto.icalUrl,
                    dto.timeZone, Role.valueOf(dto.role))));
        });
    }

    private static ApartmentRequestDto toDto(ApartmentInput input) {
        return new ApartmentRequestDto(input.name, input.bookingUrl, input.icalUrl, input.timeZone);
    }
}
