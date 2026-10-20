package com.bookman.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import org.junit.jupiter.api.Test;

/** Based on a real Booking.com export (Q-18); the UID was replaced. */
class IcalReservationParserTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private final IcalReservationParser parser = new IcalReservationParser();

    @Test
    void REQ06_realBookingExportBecomesReservationWithCheckInAndCheckOut() throws Exception {
        List<ImportedReservation> result = parser.parse(load("booking-export-real.ics"), WARSAW);

        assertThat(result).containsExactly(new ImportedReservation(
                "5f3c9e1a7b2d4c6e8f0a1b2c3d4e5f60@booking.com",
                LocalDate.of(2026, 10, 22), LocalDate.of(2026, 10, 25)));
    }

    @Test
    void REQ06_eventsWithoutUidOrEndAreSkippedNotFatal() throws Exception {
        List<ImportedReservation> result = parser.parse(load("booking-export-multi.ics"), WARSAW);

        assertThat(result).extracting(ImportedReservation::externalId).containsExactly(
                "aaaa0000000000000000000000000001@booking.com",
                "aaaa0000000000000000000000000002@booking.com");
    }

    private static Calendar load(String name) throws Exception {
        try (InputStream in = IcalReservationParserTest.class.getResourceAsStream("/ical/" + name)) {
            return new CalendarBuilder().build(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}
