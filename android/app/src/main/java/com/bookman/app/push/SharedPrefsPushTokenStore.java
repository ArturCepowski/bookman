package com.bookman.app.push;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

public class SharedPrefsPushTokenStore implements PushTokenStore {

    private static final String KEY = "registered_push_token";

    private final SharedPreferences prefs;

    public SharedPrefsPushTokenStore(Context context) {
        this.prefs = context.getSharedPreferences("push", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public String registeredToken() {
        return prefs.getString(KEY, null);
    }

    @Override
    public void saveRegisteredToken(@Nullable String token) {
        prefs.edit().putString(KEY, token).apply();
    }
}
