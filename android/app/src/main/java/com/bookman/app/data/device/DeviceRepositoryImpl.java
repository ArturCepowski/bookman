package com.bookman.app.data.device;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;
import com.bookman.app.data.api.ApiCalls;
import com.bookman.app.data.api.DeviceApi;
import com.bookman.app.data.api.DeviceRequestDto;
import java.util.concurrent.Executor;

public class DeviceRepositoryImpl implements DeviceRepository {

    private final DeviceApi api;
    private final Executor io;

    public DeviceRepositoryImpl(DeviceApi api, Executor io) {
        this.api = api;
        this.io = io;
    }

    @Override
    public void register(String pushToken, Consumer<Result<Boolean>> callback) {
        io.execute(() -> callback.accept(ApiCalls.executeNoContent(api.register(new DeviceRequestDto(pushToken)))));
    }

    @Override
    public void unregister(String pushToken, String sessionToken, Consumer<Result<Boolean>> callback) {
        io.execute(() -> callback.accept(ApiCalls.executeNoContent(
                api.unregister("Bearer " + sessionToken, new DeviceRequestDto(pushToken)))));
    }
}
