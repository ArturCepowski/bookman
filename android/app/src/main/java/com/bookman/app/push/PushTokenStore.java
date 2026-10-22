package com.bookman.app.push;

import androidx.annotation.Nullable;

/** The token last registered with the backend, needed to unregister it on logout. */
public interface PushTokenStore {

    @Nullable
    String registeredToken();

    void saveRegisteredToken(@Nullable String token);
}
