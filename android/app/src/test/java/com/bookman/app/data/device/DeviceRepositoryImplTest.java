package com.bookman.app.data.device;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.bookman.app.common.Result;
import com.bookman.app.data.api.AuthInterceptor;
import com.bookman.app.data.api.DeviceApi;
import com.bookman.app.fakes.InMemorySessionStore;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DeviceRepositoryImplTest {

    private final MockWebServer server = new MockWebServer();
    private final InMemorySessionStore session = new InMemorySessionStore("current");
    private DeviceRepositoryImpl repository;

    @Before
    public void setUp() throws IOException {
        server.start();
        DeviceApi api = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(new OkHttpClient.Builder().addInterceptor(new AuthInterceptor(session)).build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DeviceApi.class);
        repository = new DeviceRepositoryImpl(api, Runnable::run);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void REQ11_registerSendsTokenWithCurrentSession() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));
        AtomicReference<Result<Boolean>> ref = new AtomicReference<>();

        repository.register("fcm-1", ref::set);

        assertTrue(ref.get().isSuccess());
        RecordedRequest request = server.takeRequest();
        assertEquals("/api/users/me/devices", request.getPath());
        assertEquals("Bearer current", request.getHeader("Authorization"));
        assertTrue(request.getBody().readUtf8().contains("\"token\":\"fcm-1\""));
    }

    @Test
    public void REQ11_unregisterAfterLogoutUsesThePreviousSession() throws Exception {
        session.clear();
        server.enqueue(new MockResponse().setResponseCode(204));
        AtomicReference<Result<Boolean>> ref = new AtomicReference<>();

        repository.unregister("fcm-1", "previous", ref::set);

        RecordedRequest request = server.takeRequest();
        assertEquals("/api/users/me/devices/unregister", request.getPath());
        assertEquals("Bearer previous", request.getHeader("Authorization"));
    }
}
