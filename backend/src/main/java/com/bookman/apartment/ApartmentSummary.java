package com.bookman.apartment;

/** REQ-03 list row: apartment name and the caller's role in it. */
public record ApartmentSummary(Long id, String name, Role role) {
}
