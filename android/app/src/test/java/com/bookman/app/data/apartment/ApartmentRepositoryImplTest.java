package com.bookman.app.data.apartment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bookman.app.common.AppError;
import com.bookman.app.common.Result;
import com.bookman.app.data.api.ApartmentApi;
import java.io.IOException;
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

public class ApartmentRepositoryImplTest {

    private final MockWebServer server = new MockWebServer();
    private ApartmentRepositoryImpl repository;

    @Before
    public void setUp() throws IOException {
        server.start();
        ApartmentApi api = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApartmentApi.class);
        repository = new ApartmentRepositoryImpl(api, Runnable::run);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void REQ03_listIsParsedWithRoles() {
        server.enqueue(json(200, "[{\"id\":1,\"name\":\"Sunny\",\"role\":\"ADMIN\"},"
                + "{\"id\":2,\"name\":\"Loft\",\"role\":\"CLEANING_SERVICE\"}]"));
        AtomicReference<Result<List<ApartmentSummary>>> ref = new AtomicReference<>();

        repository.list(ref::set);

        assertEquals(2, ref.get().value().size());
        assertEquals(Role.CLEANING_SERVICE, ref.get().value().get(1).role);
    }

    @Test
    public void REQ04_createSendsFormAndMapsIcalRejection() throws Exception {
        server.enqueue(json(400, "{\"code\":\"ICAL_NOT_A_CALENDAR\",\"message\":\"x\",\"fieldErrors\":{}}"));
        AtomicReference<Result<ApartmentDetails>> ref = new AtomicReference<>();

        repository.create(new ApartmentInput("Sunny", "https://www.booking.com/h", "https://ical.booking.com/x",
                "Europe/Warsaw"), ref::set);

        assertEquals(AppError.ICAL_NOT_A_CALENDAR, ref.get().error());
        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertTrue(request.getBody().readUtf8().contains("\"icalUrl\":\"https://ical.booking.com/x\""));
    }

    @Test
    public void REQ05_forbiddenIsMapped() {
        server.enqueue(json(403, "{\"code\":\"FORBIDDEN\",\"message\":\"no\"}"));
        AtomicReference<Result<ApartmentDetails>> ref = new AtomicReference<>();

        repository.update(5, new ApartmentInput("x", "https://www.booking.com/h", "https://ical.booking.com/x",
                "Europe/Warsaw"), ref::set);

        assertEquals(AppError.FORBIDDEN, ref.get().error());
    }

    @Test
    public void REQ05_deleteAcceptsNoContent() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));
        AtomicReference<Result<Boolean>> ref = new AtomicReference<>();

        repository.delete(5, ref::set);

        assertTrue(ref.get().isSuccess());
        assertEquals("/api/apartments/5", server.takeRequest().getPath());
    }

    @Test
    public void NFR03_missingIcalUrlIsAllowed() {
        server.enqueue(json(200, "{\"id\":5,\"name\":\"Sunny\",\"bookingUrl\":\"https://www.booking.com/h\","
                + "\"timeZone\":\"Europe/Warsaw\",\"role\":\"CLEANING_SERVICE\"}"));
        AtomicReference<Result<ApartmentDetails>> ref = new AtomicReference<>();

        repository.get(5, ref::set);

        assertNull(ref.get().value().icalUrl);
    }

    @Test
    public void REQ11_notificationSettingsRoundTripAsHourAndMinute() throws Exception {
        server.enqueue(json(200, "{\"notPlannedTime\":\"18:30\"}"));
        server.enqueue(json(200, "{\"notDoneTime\":\"07:05\"}"));
        AtomicReference<Result<NotificationSettings>> ref = new AtomicReference<>();

        repository.notificationSettings(5, ref::set);
        assertEquals(java.time.LocalTime.of(18, 30), ref.get().value().notPlannedTime);
        assertNull(ref.get().value().notDoneTime);

        repository.updateNotificationSettings(5, new NotificationSettings(null, java.time.LocalTime.of(7, 5)), ref::set);
        assertEquals(java.time.LocalTime.of(7, 5), ref.get().value().notDoneTime);
        server.takeRequest();
        RecordedRequest put = server.takeRequest();
        assertEquals("PUT", put.getMethod());
        assertEquals("/api/apartments/5/notification-settings", put.getPath());
        String body = put.getBody().readUtf8();
        assertTrue(body.contains("\"notDoneTime\":\"07:05\""));
        assertTrue(!body.contains("notPlannedTime"));
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body);
    }
}
