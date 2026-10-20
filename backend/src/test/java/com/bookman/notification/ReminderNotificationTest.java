package com.bookman.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookman.device.DeviceTokenRepository;
import com.bookman.reservation.ImportedReservation;
import com.bookman.support.FakePushSender.Push;
import com.bookman.support.IntegrationTest;
import com.bookman.user.Language;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** REQ-11/12 reminders, delivered as push notifications to the recipients' phones (Q-24). */
class ReminderNotificationTest extends IntegrationTest {

    private static final String ICAL = "https://ical.booking.com/v1/export?t=reminders";
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);
    private static final String ADMIN_PHONE = "fcm-token-admin";
    private static final String CLEANER_PHONE = "fcm-token-cleaner";

    @Autowired ReminderPlanner planner;
    @Autowired ReminderDispatcher dispatcher;
    @Autowired ReminderNotificationRepository notifications;
    @Autowired DeviceTokenRepository devices;
    @Autowired JdbcTemplate jdbc;

    private TestUser admin;
    private TestUser cleaner;
    private long apartment;

    @BeforeEach
    void setUp() throws Exception {
        clock.set(TODAY.atTime(8, 0).atZone(WARSAW).toInstant());
        admin = registerUser("admin@example.com");
        cleaner = registerUser("cleaner@example.com");
        setLanguage(cleaner, Language.PL);
        registerDevice(admin, ADMIN_PHONE);
        registerDevice(cleaner, CLEANER_PHONE);
        apartment = createApartment(admin, ICAL);
        addCleaningService(apartment, cleaner);
    }

    @Test
    void REQ11_notPlannedReminderAtConfiguredHourForTomorrowsCheckOut() throws Exception {
        importStays(stay("tomorrow", -2, 1), stay("in-two-days", 0, 2), stay("today", -1, 0));
        settings("18:00", null);

        runAt(17, 59);
        assertThat(pushSender.sent()).isEmpty();

        runAt(18, 0);
        assertThat(pushSender.sent()).extracting(Push::deviceToken).containsExactlyInAnyOrder(ADMIN_PHONE, CLEANER_PHONE);
        Push toAdmin = pushTo(ADMIN_PHONE);
        assertThat(toAdmin.title()).contains("cleaning reminder");
        assertThat(toAdmin.body()).contains("Sunny flat").contains("Nov 11, 2026").contains("not planned");
        Push toCleaner = pushTo(CLEANER_PHONE);
        assertThat(toCleaner.title()).contains("przypomnienie");
        assertThat(toCleaner.body()).contains("Sunny flat").contains("11 lis 2026").contains("nie jest");
    }

    @Test
    void REQ11_oneReminderPerRecipientPerReservationPerDayEvenAfterRestart() throws Exception {
        importStays(stay("tomorrow", -2, 1));
        settings("18:00", null);

        runAt(18, 0);
        runAt(18, 1);
        runAt(18, 30);
        planner.planDue();
        dispatcher.dispatchDue();

        assertThat(pushSender.sent()).hasSize(2);
        assertThat(notifications.count()).isEqualTo(2);
    }

    @Test
    void REQ11_everyPhoneOfARecipientGetsIt() throws Exception {
        registerDevice(admin, "fcm-token-admin-tablet");
        importStays(stay("tomorrow", -2, 1));
        settings("18:00", null);

        runAt(18, 0);

        assertThat(pushSender.sent()).extracting(Push::deviceToken)
                .containsExactlyInAnyOrder(ADMIN_PHONE, "fcm-token-admin-tablet", CLEANER_PHONE);
        assertThat(notifications.count()).isEqualTo(2);
    }

    @Test
    void REQ11_onlyNotPlannedReservationsAreReminded() throws Exception {
        importStays(stay("a", -2, 1), stay("b", -3, 1), stay("c", -4, 1));
        setStatus("b", "TAKEN");
        setStatus("c", "PRIVATE");
        settings("18:00", null);

        runAt(18, 0);

        assertThat(pushSender.sent()).hasSize(2);
    }

    @Test
    void REQ11_disabledReminderSendsNothing() throws Exception {
        importStays(stay("tomorrow", -2, 1));
        settings(null, null);

        runAt(18, 0);

        assertThat(pushSender.sent()).isEmpty();
    }

    @Test
    void REQ11_missedHourIsCaughtUpWithinOneHourOnly() throws Exception {
        importStays(stay("tomorrow", -2, 1));
        settings("18:00", null);

        runAt(19, 1);
        assertThat(pushSender.sent()).as("more than an hour late – skipped").isEmpty();

        // Back to the morning only so the test's login token (1 h, same clock) is valid for the settings call.
        clock.set(TODAY.atTime(8, 0).atZone(WARSAW).toInstant());
        settings("18:45", null);
        runAt(19, 5);
        assertThat(pushSender.sent()).as("20 minutes late – still sent").hasSize(2);
    }

    @Test
    void REQ12_notDoneReminderForTodaysCheckOutUnlessDoneOrPrivate() throws Exception {
        importStays(stay("planned", -2, 0), stay("taken", -3, 0), stay("done", -4, 0), stay("private", -5, 0),
                stay("tomorrow", -1, 1));
        setStatus("taken", "TAKEN");
        setStatus("done", "TAKEN");
        setStatus("done", "DONE");
        setStatus("private", "PRIVATE");
        settings(null, "11:00");

        runAt(11, 0);

        assertThat(pushSender.sent()).hasSize(4);
        assertThat(pushTo(ADMIN_PHONE).body()).contains("not done").contains("Nov 10, 2026");
    }

    @Test
    void REQ12_hourIsInTheApartmentsTimeZone() throws Exception {
        importStays(stay("today", -2, 0));
        settings(null, "11:00");

        clock.set(TODAY.atTime(10, 30).atZone(ZoneId.of("UTC")).toInstant()); // 11:30 in Warsaw (CET)
        planner.planDue();
        dispatcher.dispatchDue();
        assertThat(pushSender.sent()).as("11:00 Warsaw has passed at 10:30 UTC").hasSize(2);

        pushSender.reset();
        jdbc.update("delete from reminder_notification");
        clock.set(TODAY.atTime(9, 30).atZone(ZoneId.of("UTC")).toInstant()); // 10:30 in Warsaw
        planner.planDue();
        dispatcher.dispatchDue();
        assertThat(pushSender.sent()).as("10:30 Warsaw is before 11:00").isEmpty();
    }

    @Test
    void REQ13_failedSendIsRetriedAtMostThreeTimes() throws Exception {
        importStays(stay("tomorrow", -2, 1));
        settings("18:00", null);
        pushSender.failNext(100);

        runAt(18, 0);
        for (int seconds : new int[] {2, 6, 14, 30, 60}) {
            clock.set(TODAY.atTime(18, 0).atZone(WARSAW).toInstant().plusSeconds(seconds));
            dispatcher.dispatchDue();
        }

        assertThat(pushSender.attempts()).as("2 recipients x (1 attempt + 3 retries)").isEqualTo(8);
        assertThat(notifications.findAll()).allSatisfy(n -> {
            assertThat(n.getStatus()).isEqualTo(ReminderNotification.Status.FAILED);
            assertThat(n.getAttempts()).isEqualTo(ReminderNotification.MAX_ATTEMPTS);
        });
    }

    @Test
    void REQ13_temporaryFailureIsSentOnRetry() throws Exception {
        importStays(stay("tomorrow", -2, 1));
        settings("18:00", null);
        pushSender.failNext(1);

        runAt(18, 0);
        assertThat(pushSender.sent()).hasSize(1);
        clock.set(TODAY.atTime(18, 0).atZone(WARSAW).toInstant().plusSeconds(3));
        dispatcher.dispatchDue();

        assertThat(pushSender.sent()).hasSize(2);
        assertThat(notifications.findAll())
                .allSatisfy(n -> assertThat(n.getStatus()).isEqualTo(ReminderNotification.Status.SENT));
    }

    @Test
    void REQ13_invalidDeviceTokenIsForgottenAndUserWithoutPhoneIsNotRetried() throws Exception {
        pushSender.markInvalid(CLEANER_PHONE);
        importStays(stay("tomorrow", -2, 1));
        settings("18:00", null);

        runAt(18, 0);

        assertThat(pushSender.sent()).extracting(Push::deviceToken).containsExactly(ADMIN_PHONE);
        assertThat(devices.findByToken(CLEANER_PHONE)).isEmpty();
        assertThat(notifications.findAll()).extracting(ReminderNotification::getStatus)
                .containsExactlyInAnyOrder(ReminderNotification.Status.SENT, ReminderNotification.Status.NO_DEVICE);
    }

    @Test
    void REQ11_settingsAreAdminOnlyAndValidated() throws Exception {
        String url = "/api/apartments/" + apartment + "/notification-settings";
        call(cleaner, get(url), null).andExpect(status().isForbidden());
        call(cleaner, put(url), Map.of("notPlannedTime", "18:00")).andExpect(status().isForbidden());
        call(admin, put(url), Map.of("notPlannedTime", "25:00")).andExpect(status().isBadRequest());

        call(admin, put(url), Map.of("notPlannedTime", "18:30", "notDoneTime", "07:05")).andExpect(status().isOk());
        call(admin, get(url), null)
                .andExpect(jsonPath("$.notPlannedTime").value("18:30"))
                .andExpect(jsonPath("$.notDoneTime").value("07:05"));
    }

    private static String importUrl(long apartment) {
        return "/api/apartments/" + apartment + "/reservations/import";
    }

    private static String listUrl(long apartment) {
        return "/api/apartments/" + apartment + "/reservations";
    }

    private void runAt(int hour, int minute) {
        clock.set(TODAY.atTime(LocalTime.of(hour, minute)).atZone(WARSAW).toInstant());
        planner.planDue();
        dispatcher.dispatchDue();
    }

    private void importStays(ImportedReservation... stays) throws Exception {
        reservationSource.feed(ICAL, stays);
        call(admin, post(importUrl(apartment)), null).andExpect(status().isOk());
    }

    private static ImportedReservation stay(String uid, int checkInFromToday, int checkOutFromToday) {
        return new ImportedReservation(uid, TODAY.plusDays(checkInFromToday), TODAY.plusDays(checkOutFromToday));
    }

    private void settings(String notPlanned, String notDone) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("notPlannedTime", notPlanned);
        body.put("notDoneTime", notDone);
        call(admin, put("/api/apartments/" + apartment + "/notification-settings"), body).andExpect(status().isOk());
    }

    private void setStatus(String uid, String status) throws Exception {
        long id = jdbc.queryForObject("select id from reservation where external_id = ?", Long.class, uid);
        call(admin, put(listUrl(apartment) + "/" + id + "/cleaning-status"), Map.of("status", status))
                .andExpect(status().isOk());
    }

    private void setLanguage(TestUser user, Language language) throws Exception {
        call(user, put("/api/users/me"), Map.of("phoneNumber", "+48700200300", "language", language.name()))
                .andExpect(status().isOk());
    }

    private void registerDevice(TestUser user, String token) throws Exception {
        call(user, post("/api/users/me/devices"), Map.of("token", token)).andExpect(status().isNoContent());
    }

    private Push pushTo(String token) {
        List<Push> sent = pushSender.sent();
        return sent.stream().filter(p -> p.deviceToken().equals(token)).findFirst().orElseThrow();
    }
}
