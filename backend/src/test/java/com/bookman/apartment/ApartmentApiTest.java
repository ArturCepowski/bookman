package com.bookman.apartment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookman.apartment.calendar.CalendarFeedException;
import com.bookman.support.IntegrationTest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@RecordApplicationEvents
class ApartmentApiTest extends IntegrationTest {

    private static final String ICAL = "https://ical.booking.com/v1/export?t=11111111-aaaa";
    private static final String LISTING = "https://www.booking.com/hotel/pl/sunny-flat.html";

    @Autowired ApartmentMemberRepository members;
    @Autowired ApartmentRepository apartments;
    @Autowired ApplicationEvents events;

    @Test
    void REQ03_newUserHasEmptyList() throws Exception {
        TestUser anna = registerUser("anna@example.com");

        call(anna, get("/api/apartments"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void REQ03_listShowsOnlyMyApartmentsWithMyRole() throws Exception {
        TestUser anna = registerUser("anna@example.com");
        TestUser bob = registerUser("bob@example.com");
        long annaFlat = create(anna, "Sunny flat");
        long bobFlat = create(bob, "Bob's loft");
        addCleaner(annaFlat, bob);

        call(bob, get("/api/apartments"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(bobFlat))
                .andExpect(jsonPath("$[0].name").value("Bob's loft"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[1].id").value(annaFlat))
                .andExpect(jsonPath("$[1].role").value("CLEANING_SERVICE"));
        call(anna, get("/api/apartments"), null)
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void REQ04_registrationMakesCallerAdminWithDefaultTimeZone() throws Exception {
        TestUser anna = registerUser("anna@example.com");

        call(anna, post("/api/apartments"), request("  Sunny flat  ", LISTING, ICAL, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sunny flat"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.timeZone").value("Europe/Warsaw"))
                .andExpect(jsonPath("$.icalUrl").value(ICAL));

        assertThat(calendarFeeds.verified()).extracting(Object::toString).containsExactly(ICAL);
    }

    @Test
    void REQ04_registrationTriggersFirstImport() throws Exception {
        TestUser anna = registerUser("anna@example.com");

        long id = create(anna, "Sunny flat");

        assertThat(events.stream(ApartmentRegisteredEvent.class)).containsExactly(new ApartmentRegisteredEvent(id));
    }

    @Test
    void REQ04_nameMustHave1To60Characters() throws Exception {
        TestUser anna = registerUser("anna@example.com");

        call(anna, post("/api/apartments"), request("   ", LISTING, ICAL, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
        call(anna, post("/api/apartments"), request("x".repeat(61), LISTING, ICAL, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
        call(anna, post("/api/apartments"), request("x".repeat(60), LISTING, ICAL, null))
                .andExpect(status().isCreated());
    }

    @Test
    void REQ04_onlyBookingComHttpsUrlsAreAccepted() throws Exception {
        TestUser anna = registerUser("anna@example.com");

        call(anna, post("/api/apartments"), request("Flat", "https://evil.example/hotel", ICAL, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.bookingUrl").exists());
        call(anna, post("/api/apartments"), request("Flat", LISTING, "http://169.254.169.254/latest/meta-data", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.icalUrl").exists());
        assertThat(calendarFeeds.verified()).isEmpty();
    }

    @Test
    void REQ04_unreachableOrInvalidIcalIsRejected() throws Exception {
        TestUser anna = registerUser("anna@example.com");
        String dead = "https://ical.booking.com/v1/export?t=dead";
        String html = "https://ical.booking.com/v1/export?t=html";
        calendarFeeds.failFor(dead, CalendarFeedException.Reason.UNREACHABLE);
        calendarFeeds.failFor(html, CalendarFeedException.Reason.NOT_A_CALENDAR);

        call(anna, post("/api/apartments"), request("Flat", LISTING, dead, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ICAL_UNREACHABLE"));
        call(anna, post("/api/apartments"), request("Flat", LISTING, html, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ICAL_NOT_A_CALENDAR"));
        assertThat(apartments.count()).isZero();
    }

    @Test
    void REQ04_unknownTimeZoneIsRejected() throws Exception {
        TestUser anna = registerUser("anna@example.com");

        call(anna, post("/api/apartments"), request("Flat", LISTING, ICAL, "Mars/Olympus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.timeZone").exists());
        call(anna, post("/api/apartments"), request("Flat", LISTING, ICAL, "Europe/London"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.timeZone").value("Europe/London"));
    }

    @Test
    void REQ05_adminCanEditApartment() throws Exception {
        TestUser anna = registerUser("anna@example.com");
        long id = create(anna, "Sunny flat");
        calendarFeeds.reset();

        call(anna, put("/api/apartments/" + id), request("Sunny flat 2", LISTING, ICAL, "Europe/Berlin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sunny flat 2"))
                .andExpect(jsonPath("$.timeZone").value("Europe/Berlin"));
        assertThat(calendarFeeds.verified()).as("unchanged iCal URL is not downloaded again").isEmpty();

        String newIcal = "https://ical.booking.com/v1/export?t=22222222-bbbb";
        call(anna, put("/api/apartments/" + id), request("Sunny flat 2", LISTING, newIcal, "Europe/Berlin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.icalUrl").value(newIcal));
        assertThat(calendarFeeds.verified()).extracting(Object::toString).containsExactly(newIcal);
    }

    @Test
    void REQ05_cleaningServiceCannotEditOrDeleteApartment() throws Exception {
        TestUser anna = registerUser("anna@example.com");
        TestUser cleaner = registerUser("cleaner@example.com");
        long id = create(anna, "Sunny flat");
        addCleaner(id, cleaner);

        call(cleaner, put("/api/apartments/" + id), request("Hacked", LISTING, ICAL, null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        call(cleaner, delete("/api/apartments/" + id), null)
                .andExpect(status().isForbidden());
        assertThat(apartments.findById(id)).get().extracting(Apartment::getName).isEqualTo("Sunny flat");
    }

    @Test
    void REQ05_userWithoutRoleIsForbidden() throws Exception {
        TestUser anna = registerUser("anna@example.com");
        TestUser stranger = registerUser("stranger@example.com");
        long id = create(anna, "Sunny flat");

        call(stranger, get("/api/apartments/" + id), null).andExpect(status().isForbidden());
        call(stranger, put("/api/apartments/" + id), request("X", LISTING, ICAL, null)).andExpect(status().isForbidden());
        call(stranger, delete("/api/apartments/" + id), null).andExpect(status().isForbidden());
        call(stranger, get("/api/apartments/999999"), null).andExpect(status().isForbidden());
    }

    @Test
    void REQ05_deletingApartmentRemovesItsRoles() throws Exception {
        TestUser anna = registerUser("anna@example.com");
        TestUser cleaner = registerUser("cleaner@example.com");
        long id = create(anna, "Sunny flat");
        addCleaner(id, cleaner);

        call(anna, delete("/api/apartments/" + id), null).andExpect(status().isNoContent());

        assertThat(apartments.findById(id)).isEmpty();
        assertThat(members.count()).isZero();
        call(cleaner, get("/api/apartments"), null).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void NFR03_icalUrlIsShownOnlyToAdmin() throws Exception {
        TestUser anna = registerUser("anna@example.com");
        TestUser cleaner = registerUser("cleaner@example.com");
        long id = create(anna, "Sunny flat");
        addCleaner(id, cleaner);

        call(anna, get("/api/apartments/" + id), null)
                .andExpect(jsonPath("$.icalUrl").value(ICAL))
                .andExpect(jsonPath("$.role").value("ADMIN"));
        call(cleaner, get("/api/apartments/" + id), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.icalUrl").doesNotExist())
                .andExpect(jsonPath("$.role").value("CLEANING_SERVICE"))
                .andExpect(jsonPath("$.bookingUrl").value(LISTING));
    }

    private long create(TestUser user, String name) throws Exception {
        return bodyOf(call(user, post("/api/apartments"), request(name, LISTING, ICAL, null))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    /** Invitations arrive in Stage 4 (REQ-09); until then roles are inserted directly. */
    private void addCleaner(long apartmentId, TestUser user) {
        members.save(new ApartmentMember(apartmentId, user.id(), Role.CLEANING_SERVICE, Instant.now()));
    }

    private static Map<String, Object> request(String name, String booking, String ical, String timeZone) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("bookingUrl", booking);
        body.put("icalUrl", ical);
        body.put("timeZone", timeZone);
        return body;
    }
}
