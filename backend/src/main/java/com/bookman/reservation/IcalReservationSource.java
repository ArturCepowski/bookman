package com.bookman.reservation;

import com.bookman.apartment.Apartment;
import com.bookman.apartment.calendar.CalendarFeedClient;
import com.bookman.apartment.calendar.CalendarFeedException;
import java.net.URI;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Component;

/** REQ-06 v1: periodic download of the apartment's Booking.com iCal export. */
@Component
public class IcalReservationSource implements ReservationSource {

    private final CalendarFeedClient client;
    private final IcalReservationParser parser;

    public IcalReservationSource(CalendarFeedClient client, IcalReservationParser parser) {
        this.client = client;
        this.parser = parser;
    }

    @Override
    public List<ImportedReservation> fetch(Apartment apartment) throws ReservationSourceException {
        try {
            return parser.parse(client.fetch(URI.create(apartment.getIcalUrl())), ZoneId.of(apartment.getTimeZone()));
        } catch (CalendarFeedException e) {
            throw new ReservationSourceException(e.reason() + ": " + e.getMessage());
        }
    }
}
