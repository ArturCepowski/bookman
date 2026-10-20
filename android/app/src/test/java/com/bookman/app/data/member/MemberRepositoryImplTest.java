package com.bookman.app.data.member;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.bookman.app.common.AppError;
import com.bookman.app.common.Result;
import com.bookman.app.data.api.MemberApi;
import com.bookman.app.data.apartment.ApartmentSummary;
import com.bookman.app.data.apartment.Role;
import java.io.IOException;
import java.time.Instant;
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

public class MemberRepositoryImplTest {

    private final MockWebServer server = new MockWebServer();
    private MemberRepositoryImpl repository;

    @Before
    public void setUp() throws IOException {
        server.start();
        MemberApi api = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MemberApi.class);
        repository = new MemberRepositoryImpl(api, Runnable::run);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void REQ10_overviewIsParsed() {
        server.enqueue(json(200, "{\"members\":[{\"userId\":1,\"email\":\"admin@example.com\",\"role\":\"ADMIN\"},"
                + "{\"userId\":2,\"email\":\"cleaner@example.com\",\"role\":\"CLEANING_SERVICE\"}],"
                + "\"invitations\":[{\"id\":9,\"email\":\"new@example.com\",\"expiresAt\":\"2026-11-03T10:15:30.123456Z\"}]}"));
        AtomicReference<Result<MembersOverview>> ref = new AtomicReference<>();

        repository.overview(5, ref::set);

        MembersOverview overview = ref.get().value();
        assertEquals(Role.CLEANING_SERVICE, overview.members.get(1).role);
        assertEquals(Instant.parse("2026-11-03T10:15:30.123456Z"), overview.invitations.get(0).expiresAt);
    }

    @Test
    public void REQ09_inviteSendsEmailAndMapsConflicts() throws Exception {
        server.enqueue(json(409, "{\"code\":\"INVITATION_PENDING\",\"message\":\"x\"}"));
        AtomicReference<Result<PendingInvitation>> ref = new AtomicReference<>();

        repository.invite(5, "cleaner@example.com", ref::set);

        assertEquals(AppError.INVITATION_PENDING, ref.get().error());
        RecordedRequest request = server.takeRequest();
        assertEquals("/api/apartments/5/invitations", request.getPath());
        assertTrue(request.getBody().readUtf8().contains("\"email\":\"cleaner@example.com\""));
    }

    @Test
    public void REQ09_myInvitationsAndAccept() throws Exception {
        server.enqueue(json(200, "[{\"id\":1,\"apartmentId\":5,\"apartmentName\":\"Sunny flat\","
                + "\"invitedBy\":\"admin@example.com\",\"expiresAt\":\"2026-11-03T10:00:00Z\"}]"));
        server.enqueue(json(200, "{\"id\":5,\"name\":\"Sunny flat\",\"role\":\"CLEANING_SERVICE\"}"));
        AtomicReference<Result<List<MyInvitation>>> mine = new AtomicReference<>();
        AtomicReference<Result<ApartmentSummary>> accepted = new AtomicReference<>();

        repository.myInvitations(mine::set);
        repository.accept(1, accepted::set);

        assertEquals("admin@example.com", mine.get().value().get(0).invitedBy);
        assertEquals(Role.CLEANING_SERVICE, accepted.get().value().role);
        server.takeRequest();
        assertEquals("/api/invitations/1/accept", server.takeRequest().getPath());
    }

    @Test
    public void REQ10_removeAndCancelUseDelete() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));
        server.enqueue(json(409, "{\"code\":\"CANNOT_REMOVE_ADMIN\",\"message\":\"x\"}"));
        AtomicReference<Result<Boolean>> ref = new AtomicReference<>();

        repository.cancelInvitation(5, 9, ref::set);
        assertTrue(ref.get().isSuccess());
        repository.removeMember(5, 1, ref::set);
        assertEquals(AppError.CANNOT_REMOVE_ADMIN, ref.get().error());

        RecordedRequest cancel = server.takeRequest();
        assertEquals("DELETE", cancel.getMethod());
        assertEquals("/api/apartments/5/invitations/9", cancel.getPath());
        assertEquals("/api/apartments/5/members/1", server.takeRequest().getPath());
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body);
    }
}
