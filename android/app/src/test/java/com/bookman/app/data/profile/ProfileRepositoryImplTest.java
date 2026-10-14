package com.bookman.app.data.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bookman.app.common.AppError;
import com.bookman.app.common.Result;
import com.bookman.app.data.api.AuthInterceptor;
import com.bookman.app.data.api.UserApi;
import com.bookman.app.data.auth.User;
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

public class ProfileRepositoryImplTest {

    private final MockWebServer server = new MockWebServer();
    private final InMemorySessionStore session = new InMemorySessionStore("t-1");
    private ProfileRepositoryImpl repository;

    @Before
    public void setUp() throws IOException {
        server.start();
        UserApi api = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(new OkHttpClient.Builder().addInterceptor(new AuthInterceptor(session)).build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(UserApi.class);
        repository = new ProfileRepositoryImpl(api, Runnable::run);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void REQ02_updateSendsPhoneAndLanguageButNoEmail() throws Exception {
        server.enqueue(json(200, "{\"id\":1,\"email\":\"anna@example.com\",\"phoneNumber\":\"+48700200300\","
                + "\"language\":\"PL\"}"));
        AtomicReference<Result<User>> ref = new AtomicReference<>();

        repository.update("+48700200300", "PL", ref::set);

        assertEquals("PL", ref.get().value().language());
        RecordedRequest request = server.takeRequest();
        assertEquals("PUT", request.getMethod());
        assertEquals("Bearer t-1", request.getHeader("Authorization"));
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"phoneNumber\":\"+48700200300\""));
        assertTrue(!body.contains("email"));
    }

    @Test
    public void REQ01_expiredSessionIsClearedAndReported() {
        server.enqueue(json(401, "{\"code\":\"UNAUTHORIZED\",\"message\":\"UNAUTHORIZED\"}"));
        AtomicReference<Result<User>> ref = new AtomicReference<>();

        repository.load(ref::set);

        assertEquals(AppError.SESSION_EXPIRED, ref.get().error());
        assertNull(session.token());
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body);
    }
}
