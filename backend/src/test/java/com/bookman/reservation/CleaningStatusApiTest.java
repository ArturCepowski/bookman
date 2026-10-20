package com.bookman.reservation;

import static com.bookman.reservation.ReservationImportApiTest.importUrl;
import static com.bookman.reservation.ReservationImportApiTest.listUrl;
import static com.bookman.reservation.ReservationImportApiTest.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookman.support.IntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

class CleaningStatusApiTest extends IntegrationTest {

    private static final String ICAL = "https://ical.booking.com/v1/export?t=status-test";

    @Autowired ReservationRepository reservations;
    @Autowired ReservationStatusChangeRepository history;

    private TestUser admin;
    private TestUser cleaner;
    private TestUser stranger;
    private long apartment;
    private long reservation;

    @BeforeEach
    void setUpReservation() throws Exception {
        admin = registerUser("admin@example.com");
        cleaner = registerUser("cleaner@example.com");
        stranger = registerUser("stranger@example.com");
        apartment = createApartment(admin, ICAL);
        addCleaningService(apartment, cleaner);
        reservationSource.feed(ICAL, stay("a", 1, 3));
        call(admin, post(importUrl(apartment)), null);
        reservation = bodyOf(call(admin, get(listUrl(apartment)), null)).get(0).get("id").asLong();
    }

    @Test
    void REQ08_cleaningServiceTakesAndFinishesAndIsRecordedAsResponsible() throws Exception {
        change(cleaner, "TAKEN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cleaningStatus").value("TAKEN"))
                .andExpect(jsonPath("$.takenBy").value("cleaner@example.com"));
        change(cleaner, "DONE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cleaningStatus").value("DONE"))
                .andExpect(jsonPath("$.takenBy").value("cleaner@example.com"));

        assertThat(reservations.findById(reservation).orElseThrow().getResponsibleUserId()).isEqualTo(cleaner.id());
    }

    @Test
    void REQ08_cleaningServiceCannotSkipOrRevert() throws Exception {
        change(cleaner, "DONE").andExpect(status().isForbidden());
        change(cleaner, "TAKEN").andExpect(status().isOk());
        change(cleaner, "NOT_PLANNED").andExpect(status().isForbidden());
        change(cleaner, "DONE").andExpect(status().isOk());
        change(cleaner, "TAKEN").andExpect(status().isForbidden());
        change(cleaner, "NOT_PLANNED").andExpect(status().isForbidden());
    }

    @Test
    void REQ08_adminCanSetAnyStatusIncludingRevert() throws Exception {
        change(admin, "DONE").andExpect(status().isOk());
        change(admin, "NOT_PLANNED").andExpect(status().isOk()).andExpect(jsonPath("$.takenBy").doesNotExist());
        change(admin, "TAKEN").andExpect(status().isOk()).andExpect(jsonPath("$.takenBy").value("admin@example.com"));
        change(admin, "NOT_PLANNED").andExpect(status().isOk());
    }

    @Test
    void REQ08_everyChangeStoresWhoAndWhen() throws Exception {
        Instant before = Instant.now().minus(1, ChronoUnit.SECONDS);
        change(cleaner, "TAKEN");
        change(admin, "NOT_PLANNED");

        List<ReservationStatusChange> changes = history.findByReservationIdOrderByIdAsc(reservation);
        assertThat(changes).hasSize(2);
        assertThat(changes.get(0).getFromStatus()).isEqualTo(CleaningStatus.NOT_PLANNED);
        assertThat(changes.get(0).getToStatus()).isEqualTo(CleaningStatus.TAKEN);
        assertThat(changes.get(0).getChangedBy()).isEqualTo(cleaner.id());
        assertThat(changes.get(1).getChangedBy()).isEqualTo(admin.id());
        assertThat(changes).allSatisfy(c -> assertThat(c.getChangedAt()).isAfter(before));
    }

    @Test
    void REQ08_usersWithoutRoleCannotChangeStatus() throws Exception {
        change(stranger, "TAKEN").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(history.findByReservationIdOrderByIdAsc(reservation)).isEmpty();
    }

    @Test
    void REQ08_secondPersonTakingTheSameCleaningGetsConflict() throws Exception {
        TestUser secondCleaner = registerUser("second@example.com");
        addCleaningService(apartment, secondCleaner);

        change(cleaner, "TAKEN").andExpect(status().isOk());
        change(secondCleaner, "TAKEN")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATUS_CONFLICT"));
        assertThat(reservations.findById(reservation).orElseThrow().getResponsibleUserId()).isEqualTo(cleaner.id());
    }

    @Test
    void REQ08_reservationOfAnotherApartmentOrCancelledIsNotFound() throws Exception {
        long otherApartment = createApartment(admin, ICAL + "-other");
        call(admin, put(listUrl(otherApartment) + "/" + reservation + "/cleaning-status"), Map.of("status", "TAKEN"))
                .andExpect(status().isNotFound());

        reservationSource.feed(ICAL);
        call(admin, post(importUrl(apartment)), null);
        change(admin, "TAKEN").andExpect(status().isNotFound());
    }

    private ResultActions change(TestUser user, String status) throws Exception {
        return call(user, put(listUrl(apartment) + "/" + reservation + "/cleaning-status"), Map.of("status", status));
    }
}
