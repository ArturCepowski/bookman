package com.bookman.app.ui.apartment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bookman.app.ui.common.FieldError;
import org.junit.Test;

public class ApartmentFormTest {

    private static final String LISTING = "https://www.booking.com/hotel/pl/sunny.html";
    private static final String ICAL = "https://ical.booking.com/v1/export?t=abc";

    @Test
    public void REQ04_validFormIsTrimmedAndDefaultsToWarsaw() {
        ApartmentForm form = ApartmentForm.of("  Sunny flat ", LISTING, ICAL, " ");

        assertTrue(form.isValid());
        assertEquals("Sunny flat", form.input.name);
        assertEquals("Europe/Warsaw", form.input.timeZone);
    }

    @Test
    public void REQ04_nameMustHave1To60Characters() {
        assertEquals(FieldError.NAME_INVALID, ApartmentForm.of("  ", LISTING, ICAL, "").nameError);
        assertEquals(FieldError.NAME_INVALID, ApartmentForm.of(repeat(61), LISTING, ICAL, "").nameError);
        assertNull(ApartmentForm.of(repeat(60), LISTING, ICAL, "").nameError);
    }

    @Test
    public void REQ04_linksMustBeHttpsBookingCom() {
        ApartmentForm form = ApartmentForm.of("Flat", "https://evil.example/x", "http://ical.booking.com/x", "");

        assertFalse(form.isValid());
        assertEquals(FieldError.BOOKING_URL_INVALID, form.bookingUrlError);
        assertEquals(FieldError.ICAL_URL_INVALID, form.icalUrlError);
        assertEquals(FieldError.ICAL_URL_INVALID,
                ApartmentForm.of("Flat", LISTING, "https://booking.com.evil.example/x", "").icalUrlError);
    }

    @Test
    public void REQ04_timeZoneMustExist() {
        assertEquals(FieldError.TIME_ZONE_INVALID, ApartmentForm.of("Flat", LISTING, ICAL, "Mars/Olympus").timeZoneError);
        assertNull(ApartmentForm.of("Flat", LISTING, ICAL, "Europe/London").timeZoneError);
    }

    private static String repeat(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append('x');
        }
        return sb.toString();
    }
}
