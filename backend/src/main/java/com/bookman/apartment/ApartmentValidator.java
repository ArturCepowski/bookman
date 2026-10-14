package com.bookman.apartment;

import com.bookman.apartment.calendar.CalendarFeedException;
import com.bookman.apartment.calendar.CalendarFeedVerifier;
import com.bookman.common.ApiException;
import com.bookman.common.ErrorCode;
import java.net.URI;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ApartmentValidator {

    static final String DEFAULT_TIME_ZONE = "Europe/Warsaw";

    private final ApartmentUrlPolicy urlPolicy;
    private final CalendarFeedVerifier calendarFeedVerifier;

    public ApartmentValidator(ApartmentUrlPolicy urlPolicy, CalendarFeedVerifier calendarFeedVerifier) {
        this.urlPolicy = urlPolicy;
        this.calendarFeedVerifier = calendarFeedVerifier;
    }

    /**
     * @param previousIcalUrl when editing, the stored URL; an unchanged URL is not downloaded again
     */
    public ApartmentData validate(ApartmentRequest request, String previousIcalUrl) {
        String name = request.name().trim();
        if (name.isEmpty()) {
            throw ApiException.invalidField("name", "must not be blank");
        }
        URI booking = urlPolicy.parseAllowed(request.bookingUrl())
                .orElseThrow(() -> ApiException.invalidField("bookingUrl", "must be an https link to booking.com"));
        URI ical = urlPolicy.parseAllowed(request.icalUrl())
                .orElseThrow(() -> ApiException.invalidField("icalUrl", "must be an https Booking.com calendar export link"));
        String timeZone = timeZone(request.timeZone());

        if (!ical.toString().equals(previousIcalUrl)) {
            verifyCalendar(ical);
        }
        return new ApartmentData(name, booking.toString(), ical.toString(), timeZone);
    }

    private static String timeZone(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_TIME_ZONE;
        }
        try {
            return ZoneId.of(value.trim()).getId();
        } catch (DateTimeException e) {
            throw ApiException.invalidField("timeZone", "unknown time zone");
        }
    }

    private void verifyCalendar(URI ical) {
        try {
            calendarFeedVerifier.verify(ical);
        } catch (CalendarFeedException e) {
            ErrorCode code = e.reason() == CalendarFeedException.Reason.UNREACHABLE
                    ? ErrorCode.ICAL_UNREACHABLE
                    : ErrorCode.ICAL_NOT_A_CALENDAR;
            throw new ApiException(HttpStatus.BAD_REQUEST, code, "Calendar export URL rejected",
                    Map.of("icalUrl", code.name()));
        }
    }
}
