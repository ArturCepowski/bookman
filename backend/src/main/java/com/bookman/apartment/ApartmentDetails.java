package com.bookman.apartment;

import com.fasterxml.jackson.annotation.JsonInclude;

/** {@code icalUrl} is present only for ADMIN callers (NFR-03). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApartmentDetails(Long id, String name, String bookingUrl, String icalUrl, String timeZone, Role role) {

    static ApartmentDetails of(Apartment a, Role role) {
        return new ApartmentDetails(a.getId(), a.getName(), a.getBookingUrl(),
                role == Role.ADMIN ? a.getIcalUrl() : null, a.getTimeZone(), role);
    }
}
