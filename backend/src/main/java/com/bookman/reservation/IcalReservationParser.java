package com.bookman.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Q-08: strict only about the fields we use. An event without UID, DTSTART or DTEND is skipped with a
 * warning instead of failing the import. Booking.com exports whole days: DTSTART = check-in, DTEND = check-out.
 * Every event becomes a reservation – the export does not distinguish guest bookings from dates closed by hand
 * (all are "CLOSED - Not available"); the admin marks the latter as PRIVATE (Q-20).
 */
@org.springframework.stereotype.Component
public class IcalReservationParser {

    private static final Logger log = LoggerFactory.getLogger(IcalReservationParser.class);

    public List<ImportedReservation> parse(Calendar calendar, ZoneId zone) {
        List<ImportedReservation> result = new ArrayList<>();
        for (VEvent event : calendar.<VEvent>getComponents(Component.VEVENT)) {
            Optional<String> uid = event.getUid().map(u -> u.getValue()).filter(v -> !v.isBlank());
            Optional<LocalDate> start = event.<DtStart<?>>getProperty(Property.DTSTART).map(p -> toDate(p, zone));
            Optional<LocalDate> end = event.<DtEnd<?>>getProperty(Property.DTEND).map(p -> toDate(p, zone));
            if (uid.isEmpty() || start.isEmpty() || end.isEmpty() || end.get().isBefore(start.get())) {
                log.warn("Skipping calendar event without usable UID/DTSTART/DTEND: uid={}", uid.orElse("<none>"));
                continue;
            }
            result.add(new ImportedReservation(uid.get(), start.get(), end.get()));
        }
        return result;
    }

    private static LocalDate toDate(DateProperty<?> property, ZoneId zone) {
        Temporal value = property.getDate();
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toLocalDate();
        }
        if (value instanceof ZonedDateTime zoned) {
            return zoned.withZoneSameInstant(zone).toLocalDate();
        }
        if (value instanceof OffsetDateTime offset) {
            return offset.atZoneSameInstant(zone).toLocalDate();
        }
        return LocalDate.from(value);
    }
}
