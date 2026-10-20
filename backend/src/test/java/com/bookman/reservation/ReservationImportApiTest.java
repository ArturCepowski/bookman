package com.bookman.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookman.apartment.ApartmentRepository;
import com.bookman.support.IntegrationTest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

class ReservationImportApiTest extends IntegrationTest {

    private static final String ICAL = "https://ical.booking.com/v1/export?t=import-test";
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Europe/Warsaw"));

    @Autowired ReservationRepository reservations;
    @Autowired ApartmentRepository apartments;
    @Autowired Environment environment;

    @Test
    void REQ06_importTurnsEachBookingIntoReservation() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        long apartment = createApartment(admin, ICAL);
        reservationSource.feed(ICAL, stay("a", 1, 3), stay("b", 5, 8));

        call(admin, post(importUrl(apartment)), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.added").value(2));

        call(admin, get(listUrl(apartment)), null)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].checkIn").value(TODAY.plusDays(1).toString()))
                .andExpect(jsonPath("$[0].checkOut").value(TODAY.plusDays(3).toString()))
                .andExpect(jsonPath("$[0].cleaningStatus").value("NOT_PLANNED"));
    }

    @Test
    void REQ06_reimportDoesNotCreateDuplicates() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        long apartment = createApartment(admin, ICAL);
        reservationSource.feed(ICAL, stay("a", 1, 3), stay("b", 5, 8));

        call(admin, post(importUrl(apartment)), null).andExpect(jsonPath("$.added").value(2));
        call(admin, post(importUrl(apartment)), null)
                .andExpect(jsonPath("$.added").value(0))
                .andExpect(jsonPath("$.updated").value(0));

        assertThat(reservations.findByApartmentId(apartment)).hasSize(2);
    }

    @Test
    void REQ06_changedDatesUpdateTheSameReservation() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        long apartment = createApartment(admin, ICAL);
        reservationSource.feed(ICAL, stay("a", 1, 3));
        call(admin, post(importUrl(apartment)), null);

        reservationSource.feed(ICAL, stay("a", 2, 6));
        call(admin, post(importUrl(apartment)), null).andExpect(jsonPath("$.updated").value(1));

        List<Reservation> stored = reservations.findByApartmentId(apartment);
        assertThat(stored).singleElement().satisfies(r -> {
            assertThat(r.getCheckIn()).isEqualTo(TODAY.plusDays(2));
            assertThat(r.getCheckOut()).isEqualTo(TODAY.plusDays(6));
        });
    }

    @Test
    void REQ06_disappearedFutureReservationIsCancelledAndHidden() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        long apartment = createApartment(admin, ICAL);
        reservationSource.feed(ICAL, stay("future", 4, 6), stay("ends-today", -2, 0), stay("past", -10, -7));
        call(admin, post(importUrl(apartment)), null);

        reservationSource.feed(ICAL);
        call(admin, post(importUrl(apartment)), null).andExpect(jsonPath("$.cancelled").value(2));

        assertThat(byExternalId(apartment, "future").isCancelled()).isTrue();
        assertThat(byExternalId(apartment, "ends-today").isCancelled()).as("check-out today = not ended yet").isTrue();
        assertThat(byExternalId(apartment, "past").isCancelled()).as("ended stays are left alone").isFalse();
        call(admin, get(listUrl(apartment)), null).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void REQ06_cancelledReservationThatReappearsIsActiveAgain() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        long apartment = createApartment(admin, ICAL);
        reservationSource.feed(ICAL, stay("a", 1, 3));
        call(admin, post(importUrl(apartment)), null);
        reservationSource.feed(ICAL);
        call(admin, post(importUrl(apartment)), null);

        reservationSource.feed(ICAL, stay("a", 1, 3));
        call(admin, post(importUrl(apartment)), null);

        assertThat(byExternalId(apartment, "a").isCancelled()).isFalse();
        call(admin, get(listUrl(apartment)), null).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void REQ06_failedDownloadChangesNothing() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        long apartment = createApartment(admin, ICAL);
        reservationSource.feed(ICAL, stay("a", 1, 3));
        call(admin, post(importUrl(apartment)), null);

        reservationSource.fail(ICAL);
        call(admin, post(importUrl(apartment)), null)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("IMPORT_FAILED"));

        assertThat(byExternalId(apartment, "a").isCancelled()).isFalse();
    }

    @Test
    void REQ06_registrationImportsImmediately() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        reservationSource.feed(ICAL, stay("a", 1, 3));

        long apartment = createApartment(admin, ICAL);

        call(admin, get(listUrl(apartment)), null).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void REQ06_anyMemberCanRefreshButStrangersCannot() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        TestUser cleaner = registerUser("cleaner@example.com");
        TestUser stranger = registerUser("stranger@example.com");
        long apartment = createApartment(admin, ICAL);
        addCleaningService(apartment, cleaner);

        call(cleaner, post(importUrl(apartment)), null).andExpect(status().isOk());
        call(stranger, post(importUrl(apartment)), null).andExpect(status().isForbidden());
    }

    @Test
    void REQ06_scheduleRunsEvery30MinutesForEveryApartment() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        long first = createApartment(admin, ICAL);
        long second = createApartment(admin, ICAL + "-2");
        List<Long> submitted = new ArrayList<>();

        new ReservationImportScheduler(apartments, submitted::add).importAll();

        assertThat(submitted).containsExactly(first, second);
        assertThat(environment.getProperty("bookman.import.interval")).isEqualTo("PT30M");
    }

    @Test
    void REQ05_deletingApartmentRemovesItsReservations() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        long apartment = createApartment(admin, ICAL);
        reservationSource.feed(ICAL, stay("a", 1, 3));
        call(admin, post(importUrl(apartment)), null);

        call(admin, delete("/api/apartments/" + apartment), null).andExpect(status().isNoContent());

        assertThat(reservations.findByApartmentId(apartment)).isEmpty();
    }

    private Reservation byExternalId(long apartment, String externalId) {
        return reservations.findByApartmentId(apartment).stream()
                .filter(r -> r.getExternalId().equals(externalId)).findFirst().orElseThrow();
    }

    static ImportedReservation stay(String uid, int checkInFromToday, int checkOutFromToday) {
        return new ImportedReservation(uid, TODAY.plusDays(checkInFromToday), TODAY.plusDays(checkOutFromToday));
    }

    static String importUrl(long apartment) {
        return "/api/apartments/" + apartment + "/reservations/import";
    }

    static String listUrl(long apartment) {
        return "/api/apartments/" + apartment + "/reservations";
    }
}
