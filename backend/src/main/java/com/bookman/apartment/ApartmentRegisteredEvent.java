package com.bookman.apartment;

/** REQ-04: published on registration; the reservation import (REQ-06) listens after commit. */
public record ApartmentRegisteredEvent(long apartmentId) {
}
