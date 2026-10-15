package com.bookman.app.data.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bookman.app.common.AppError;
import com.bookman.app.common.Result;
import com.bookman.app.data.api.AuthApi;
import com.bookman.app.data.api.AuthInterceptor;
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

public class AuthRepositoryImplTest {

    private final MockWebServer server = new MockWebServer();
    private final InMemorySessionStore session = new InMemorySessionStore();
    private AuthRepositoryImpl repository;

    @Before
    public void setUp() throws IOException {
        server.start();
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new AuthInterceptor(session)).build();
        AuthApi api = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AuthApi.class);
        repository = new AuthRepositoryImpl(api, session, Runnable::run);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void REQ01_registerStoresTokenAndSendsAllFields() throws Exception {
        server.enqueue(json(201, "{\"token\":\"t-1\",\"user\":{\"id\":7,\"email\":\"anna@example.com\","
                + "\"phoneNumber\":\"+48600100200\",\"language\":\"PL\"}}"));

        Result<User> result = call(cb -> repository.register("anna@example.com", "secret123", "+48600100200", "PL", cb));

        assertTrue(result.isSuccess());
        assertEquals(7, result.value().id());
        assertEquals("t-1", session.token());
        assertTrue(repository.isLoggedIn());
        RecordedRequest request = server.takeRequest();
        assertEquals("/api/auth/register", request.getPath());
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"phoneNumber\":\"+48600100200\""));
        assertTrue(body.contains("\"language\":\"PL\""));
    }

    @Test
    public void REQ01_duplicateEmailMapsToEmailTaken() {
        server.enqueue(json(409, "{\"code\":\"EMAIL_TAKEN\",\"message\":\"taken\"}"));

        Result<User> result = call(cb -> repository.register("anna@example.com", "secret123", "+48600100200", "EN", cb));

        assertEquals(AppError.EMAIL_TAKEN, result.error());
        assertNull(session.token());
    }

    @Test
    public void REQ01_wrongPasswordMapsToInvalidCredentials() {
        server.enqueue(json(401, "{\"code\":\"INVALID_CREDENTIALS\",\"message\":\"bad\"}"));

        Result<User> result = call(cb -> repository.login("anna@example.com", "nope", cb));

        assertEquals(AppError.INVALID_CREDENTIALS, result.error());
    }

    @Test
    public void REQ01_unreachableServerMapsToNetworkError() throws IOException {
        server.shutdown();

        Result<User> result = call(cb -> repository.login("anna@example.com", "secret123", cb));

        assertEquals(AppError.NETWORK, result.error());
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body);
    }

    private static Result<User> call(java.util.function.Consumer<androidx.core.util.Consumer<Result<User>>> action) {
        AtomicReference<Result<User>> ref = new AtomicReference<>();
        action.accept(ref::set);
        return ref.get();
    }
}
