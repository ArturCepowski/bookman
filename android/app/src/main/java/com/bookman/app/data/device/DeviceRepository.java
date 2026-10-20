package com.bookman.app.data.device;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;

/** Q-24: tells the backend which phone (FCM token) belongs to the logged-in user. */
public interface DeviceRepository {

    void register(String pushToken, Consumer<Result<Boolean>> callback);

    void unregister(String pushToken, String sessionToken, Consumer<Result<Boolean>> callback);
}
