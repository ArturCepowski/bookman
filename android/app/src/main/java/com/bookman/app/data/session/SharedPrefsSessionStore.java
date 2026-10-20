package com.bookman.app.data.session;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

/**
 * Token lives in app-private storage; android:allowBackup="false" keeps it out of device backups.
 * The token is a bearer credential with a server-side expiry (REQ-01).
 */
public class SharedPrefsSessionStore implements SessionStore {

    private static final String FILE = "session";
    private static final String KEY_TOKEN = "token";

    private final SharedPreferences prefs;

    public SharedPrefsSessionStore(Context context) {
        this.prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public String token() {
        return prefs.getString(KEY_TOKEN, null);
    }

    @Override
    public void save(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    @Override
    public void clear() {
        prefs.edit().remove(KEY_TOKEN).apply();
    }
}
