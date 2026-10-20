package com.bookman.app.push;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Result;
import com.bookman.app.data.device.DeviceRepository;
import com.bookman.app.fakes.InMemorySessionStore;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Q-24: reminders reach only phones registered for the logged-in user. */
public class PushRegistrarTest {

    private final FakeDevices devices = new FakeDevices();
    private final InMemoryTokenStore store = new InMemoryTokenStore();
    private final InMemorySessionStore session = new InMemorySessionStore("session-1");
    private String firebaseToken = "fcm-1";

    private PushRegistrar registrar() {
        return new PushRegistrar(callback -> callback.accept(firebaseToken), devices, store, session);
    }

    @Test
    public void REQ11_loggedInPhoneIsRegistered() {
        registrar().onLoggedIn();

        assertEquals(List.of("register:fcm-1"), devices.calls);
        assertEquals("fcm-1", store.registeredToken());
    }

    @Test
    public void REQ11_rotatedTokenIsRegisteredOnlyWhenLoggedIn() {
        PushRegistrar registrar = registrar();
        registrar.onNewToken("fcm-2");
        session.clear();
        registrar.onNewToken("fcm-3");

        assertEquals(List.of("register:fcm-2"), devices.calls);
    }

    @Test
    public void REQ11_logoutUnregistersWithThePreviousSession() {
        PushRegistrar registrar = registrar();
        registrar.onLoggedIn();
        devices.calls.clear();

        registrar.onLoggedOut("session-1");

        assertEquals(List.of("unregister:fcm-1:session-1"), devices.calls);
        assertNull(store.registeredToken());
    }

    @Test
    public void REQ11_buildWithoutFirebaseSimplySkipsRegistration() {
        firebaseToken = null;

        registrar().registerCurrentDevice();

        assertTrue(devices.calls.isEmpty());
    }

    @Test
    public void REQ11_failedRegistrationIsNotRemembered() {
        devices.result = Result.failure(AppError.NETWORK);

        registrar().registerCurrentDevice();

        assertNull(store.registeredToken());
    }

    private static final class FakeDevices implements DeviceRepository {
        final List<String> calls = new ArrayList<>();
        Result<Boolean> result = Result.success(true);

        @Override
        public void register(String pushToken, Consumer<Result<Boolean>> callback) {
            calls.add("register:" + pushToken);
            callback.accept(result);
        }

        @Override
        public void unregister(String pushToken, String sessionToken, Consumer<Result<Boolean>> callback) {
            calls.add("unregister:" + pushToken + ":" + sessionToken);
            callback.accept(result);
        }
    }

    private static final class InMemoryTokenStore implements PushTokenStore {
        private String token;

        @Nullable
        @Override
        public String registeredToken() {
            return token;
        }

        @Override
        public void saveRegisteredToken(@Nullable String token) {
            this.token = token;
        }
    }
}
