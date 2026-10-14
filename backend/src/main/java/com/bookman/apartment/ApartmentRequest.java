package com.bookman.apartment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** REQ-04 / REQ-05 input. URL and time-zone rules are checked in {@link ApartmentValidator}. */
public record ApartmentRequest(
        @NotBlank @Size(max = 60) String name,
        @NotBlank @Size(max = 2048) String bookingUrl,
        @NotBlank @Size(max = 2048) String icalUrl,
        String timeZone) {
}
