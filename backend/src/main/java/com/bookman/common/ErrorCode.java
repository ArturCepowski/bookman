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
    STATUS_CONFLICT,
    /** REQ-09: the invited e-mail already has a role in the apartment. */
    ALREADY_MEMBER,
    /** REQ-09: there is already a pending invitation for this e-mail. */
    INVITATION_PENDING,
    /** REQ-09: invitations expire after 14 days. */
    INVITATION_EXPIRED,
    /** REQ-10: only CLEANING_SERVICE members can be removed; the single admin stays. */
    CANNOT_REMOVE_ADMIN
}
