package com.bookman.reservation;

import static com.bookman.reservation.ReservationImportApiTest.importUrl;
import static com.bookman.reservation.ReservationImportApiTest.listUrl;
import static com.bookman.reservation.ReservationImportApiTest.stay;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookman.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReservationListApiTest extends IntegrationTest {

    private static final String ICAL = "https://ical.booking.com/v1/export?t=list-test";

    @Test
    void REQ07_showsOnlyCurrentAndFutureSortedByCheckOut() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        long apartment = createApartment(admin, ICAL);
        reservationSource.feed(ICAL,
                stay("later", 10, 14), stay("past", -6, -1), stay("checkout-today", -3, 0), stay("soon", 2, 4));
        call(admin, post(importUrl(apartment)), null);

        call(admin, get(listUrl(apartment)), null)
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].checkOut").value(ReservationImportApiTest.stay("x", 0, 0).checkOut().toString()))
                .andExpect(jsonPath("$[1].checkOut").value(stay("x", 0, 4).checkOut().toString()))
                .andExpect(jsonPath("$[2].checkOut").value(stay("x", 0, 14).checkOut().toString()));
    }

    @Test
    void REQ07_rowShowsWhoTookTheCleaning() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        TestUser cleaner = registerUser("cleaner@example.com");
        long apartment = createApartment(admin, ICAL);
        addCleaningService(apartment, cleaner);
        reservationSource.feed(ICAL, stay("a", 1, 3), stay("b", 5, 8));
        call(admin, post(importUrl(apartment)), null);
        long first = bodyOf(call(admin, get(listUrl(apartment)), null)).get(0).get("id").asLong();

        call(cleaner, put(listUrl(apartment) + "/" + first + "/cleaning-status"), Map.of("status", "TAKEN"));

        call(admin, get(listUrl(apartment)), null)
                .andExpect(jsonPath("$[0].cleaningStatus").value("TAKEN"))
                .andExpect(jsonPath("$[0].takenBy").value("cleaner@example.com"))
                .andExpect(jsonPath("$[1].cleaningStatus").value("NOT_PLANNED"))
                .andExpect(jsonPath("$[1].takenBy").doesNotExist());
    }

    @Test
    void REQ07_visibleToAdminAndCleaningServiceOnly() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        TestUser cleaner = registerUser("cleaner@example.com");
        TestUser stranger = registerUser("stranger@example.com");
        long apartment = createApartment(admin, ICAL);
        addCleaningService(apartment, cleaner);
        reservationSource.feed(ICAL, stay("a", 1, 3));
        call(admin, post(importUrl(apartment)), null);

        call(admin, get(listUrl(apartment)), null).andExpect(jsonPath("$.length()").value(1));
        call(cleaner, get(listUrl(apartment)), null).andExpect(jsonPath("$.length()").value(1));
        call(stranger, get(listUrl(apartment)), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
