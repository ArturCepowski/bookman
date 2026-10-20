package com.bookman.app.data.reservation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bookman.app.common.AppError;
import com.bookman.app.common.Result;
import com.bookman.app.data.api.ReservationApi;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ReservationRepositoryImplTest {

    private final MockWebServer server = new MockWebServer();
    private ReservationRepositoryImpl repository;

    @Before
    public void setUp() throws IOException {
        server.start();
        ReservationApi api = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ReservationApi.class);
        repository = new ReservationRepositoryImpl(api, Runnable::run);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void REQ07_listIsParsed() {
        server.enqueue(json(200, "[{\"id\":1,\"checkIn\":\"2026-10-22\",\"checkOut\":\"2026-10-25\","
                + "\"cleaningStatus\":\"TAKEN\",\"takenBy\":\"cleaner@example.com\"},"
                + "{\"id\":2,\"checkIn\":\"2026-10-26\",\"checkOut\":\"2026-10-28\",\"cleaningStatus\":\"NOT_PLANNED\"}]"));
        AtomicReference<Result<List<Reservation>>> ref = new AtomicReference<>();

        repository.list(5, ref::set);

        List<Reservation> items = ref.get().value();
        assertEquals(LocalDate.of(2026, 10, 25), items.get(0).checkOut);
        assertEquals(CleaningStatus.TAKEN, items.get(0).cleaningStatus);
        assertEquals("cleaner@example.com", items.get(0).takenBy);
        assertNull(items.get(1).takenBy);
    }

    @Test
    public void REQ06_importCallsBackendAndMapsFailure() throws Exception {
        server.enqueue(json(502, "{\"code\":\"IMPORT_FAILED\",\"message\":\"x\"}"));
        AtomicReference<Result<Boolean>> ref = new AtomicReference<>();

        repository.importNow(5, ref::set);

        assertEquals(AppError.IMPORT_FAILED, ref.get().error());
        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/apartments/5/reservations/import", request.getPath());
    }

    @Test
    public void REQ08_statusChangeSendsRequestedStatus() throws Exception {
        server.enqueue(json(200, "{\"id\":1,\"checkIn\":\"2026-10-22\",\"checkOut\":\"2026-10-25\","
                + "\"cleaningStatus\":\"DONE\",\"takenBy\":\"cleaner@example.com\"}"));
        AtomicReference<Result<Reservation>> ref = new AtomicReference<>();

        repository.changeCleaningStatus(5, 1, CleaningStatus.DONE, ref::set);

        assertEquals(CleaningStatus.DONE, ref.get().value().cleaningStatus);
        RecordedRequest request = server.takeRequest();
        assertEquals("PUT", request.getMethod());
        assertEquals("/api/apartments/5/reservations/1/cleaning-status", request.getPath());
        assertTrue(request.getBody().readUtf8().contains("\"status\":\"DONE\""));
    }

    @Test
    public void REQ08_conflictAndForbiddenAreMapped() {
        server.enqueue(json(409, "{\"code\":\"STATUS_CONFLICT\",\"message\":\"x\"}"));
        server.enqueue(json(403, "{\"code\":\"FORBIDDEN\",\"message\":\"x\"}"));
        AtomicReference<Result<Reservation>> ref = new AtomicReference<>();

        repository.changeCleaningStatus(5, 1, CleaningStatus.TAKEN, ref::set);
        assertEquals(AppError.STATUS_CONFLICT, ref.get().error());
        repository.changeCleaningStatus(5, 1, CleaningStatus.NOT_PLANNED, ref::set);
        assertEquals(AppError.FORBIDDEN, ref.get().error());
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body);
    }
}
