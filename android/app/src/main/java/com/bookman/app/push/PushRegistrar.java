package com.bookman.app.push;

import androidx.annotation.Nullable;
import com.bookman.app.data.device.DeviceRepository;
import com.bookman.app.data.session.SessionListener;
import com.bookman.app.data.session.SessionStore;

/**
 * Q-24: keeps the backend's list of this phone's push token in sync with who is logged in. Reminders (REQ-11/12)
 * are only delivered to phones registered here.
 */
public class PushRegistrar implements SessionListener {

    private final PushTokenProvider tokens;
    private final DeviceRepository devices;
    private final PushTokenStore store;
    private final SessionStore session;

    public PushRegistrar(PushTokenProvider tokens, DeviceRepository devices, PushTokenStore store, SessionStore session) {
        this.tokens = tokens;
        this.devices = devices;
        this.store = store;
        this.session = session;
    }

    /** After login, on app start (the token may have rotated while the app was closed). */
    public void registerCurrentDevice() {
        if (session.token() == null) {
            return;
        }
        tokens.currentToken(this::register);
    }

    /** FCM issued a new token for this phone. */
    public void onNewToken(String token) {
        if (session.token() != null) {
            register(token);
        }
    }

    @Override
    public void onLoggedIn() {
        registerCurrentDevice();
    }

    @Override
    public void onLoggedOut(String previousSessionToken) {
        String token = store.registeredToken();
        store.saveRegisteredToken(null);
        if (token != null && previousSessionToken != null) {
            devices.unregister(token, previousSessionToken, result -> {
                // Best effort: if it fails, the backend reassigns the token at the next login on this phone.
            });
        }
    }

    private void register(@Nullable String token) {
        if (token == null) {
            return;
        }
        devices.register(token, result -> {
            if (result.isSuccess()) {
                store.saveRegisteredToken(token);
            }
        });
    }
}
