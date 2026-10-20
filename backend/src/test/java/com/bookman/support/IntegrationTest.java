package com.bookman.support;

import com.bookman.apartment.ApartmentMember;
import com.bookman.apartment.ApartmentMemberRepository;
import com.bookman.apartment.ApartmentRepository;
import com.bookman.apartment.Role;
import com.bookman.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Full-stack tests against a real PostgreSQL 16 (embedded binaries, no Docker needed). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestBeans.class)
public abstract class IntegrationTest {

    private static final EmbeddedPostgres POSTGRES = start();

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected FakeCalendarFeedVerifier calendarFeeds;
    @Autowired protected FakeReservationSource reservationSource;
    @Autowired protected FakeSmsSender smsSender;
    @Autowired protected MutableClock clock;
    @Autowired private UserRepository users;
    @Autowired private ApartmentRepository apartments;
    @Autowired private ApartmentMemberRepository members;

    private static EmbeddedPostgres start() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @BeforeEach
    void cleanDatabase() {
        apartments.deleteAll();
        users.deleteAll();
        calendarFeeds.reset();
        reservationSource.reset();
        smsSender.reset();
        clock.reset();
    }

    /** A registered user with a valid session. */
    protected record TestUser(long id, String token) {
    }

    protected TestUser registerUser(String email) throws Exception {
        String body = json.writeValueAsString(Map.of(
                "email", email, "password", "secret123", "phoneNumber", "+48600100200", "language", "EN"));
        JsonNode node = json.readTree(mvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString());
        return new TestUser(node.get("user").get("id").asLong(), node.get("token").asText());
    }

    protected ResultActions call(TestUser user, MockHttpServletRequestBuilder request, Object body) throws Exception {
        request.header("Authorization", "Bearer " + user.token());
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(request);
    }

    /** Registers an apartment as {@code admin} with the given iCal URL and returns its id. */
    protected long createApartment(TestUser admin, String icalUrl) throws Exception {
        Map<String, Object> body = Map.of("name", "Sunny flat",
                "bookingUrl", "https://www.booking.com/hotel/pl/sunny.html", "icalUrl", icalUrl);
        return bodyOf(call(admin, MockMvcRequestBuilders.post("/api/apartments"), body)).get("id").asLong();
    }

    /** Invitations arrive in Stage 4 (REQ-09); until then roles are inserted directly. */
    protected void addCleaningService(long apartmentId, TestUser user) {
        members.save(new ApartmentMember(apartmentId, user.id(), Role.CLEANING_SERVICE, Instant.now()));
    }

    protected JsonNode bodyOf(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
