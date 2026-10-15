package com.bookman.app.ui.apartment;

import androidx.annotation.Nullable;
import com.bookman.app.data.apartment.ApartmentInput;
import com.bookman.app.ui.common.FieldError;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;

/** Client-side mirror of the backend rules (REQ-04); the backend validates again and checks the feed. */
public final class ApartmentForm {

    public static final String DEFAULT_TIME_ZONE = "Europe/Warsaw";
    static final int MAX_NAME_LENGTH = 60;

    public final ApartmentInput input;
    @Nullable public final FieldError nameError;
    @Nullable public final FieldError bookingUrlError;
    @Nullable public final FieldError icalUrlError;
    @Nullable public final FieldError timeZoneError;

    private ApartmentForm(ApartmentInput input) {
        this.input = input;
        this.nameError = input.name.isEmpty() || input.name.length() > MAX_NAME_LENGTH ? FieldError.NAME_INVALID : null;
        this.bookingUrlError = isBookingUrl(input.bookingUrl) ? null : FieldError.BOOKING_URL_INVALID;
        this.icalUrlError = isBookingUrl(input.icalUrl) ? null : FieldError.ICAL_URL_INVALID;
        this.timeZoneError = isZone(input.timeZone) ? null : FieldError.TIME_ZONE_INVALID;
    }

    public static ApartmentForm of(String name, String bookingUrl, String icalUrl, String timeZone) {
        String zone = trim(timeZone);
        return new ApartmentForm(new ApartmentInput(trim(name), trim(bookingUrl), trim(icalUrl),
                zone.isEmpty() ? DEFAULT_TIME_ZONE : zone));
    }

    public boolean isValid() {
        return nameError == null && bookingUrlError == null && icalUrlError == null && timeZoneError == null;
    }

    /** REQ-04 / Q-17 (confirmed): https links on booking.com only, same rule as the backend. */
    static boolean isBookingUrl(String value) {
        try {
            URI uri = new URI(value);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && (host.equals("booking.com") || host.endsWith(".booking.com"));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static boolean isZone(String value) {
        try {
            ZoneId.of(value);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    private static String trim(@Nullable String s) {
        return s == null ? "" : s.trim();
    }
}
