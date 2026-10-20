package com.bookman.common;

/** Stable machine-readable error codes; the Android app maps them to localized messages. */
public enum ErrorCode {
    VALIDATION_FAILED,
    EMAIL_TAKEN,
    INVALID_CREDENTIALS,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    ICAL_UNREACHABLE,
    ICAL_NOT_A_CALENDAR,
    /** Reservations could not be refreshed from the reservation source (REQ-06). */
    IMPORT_FAILED,
    /** The cleaning status was already changed by someone else (REQ-08). */
    STATUS_CONFLICT
}
