package com.bookman.apartment;

/** Validated, normalized apartment fields. */
public record ApartmentData(String name, String bookingUrl, String icalUrl, String timeZone) {
}
