package com.bookman.app.common;

/** Failure categories the UI knows how to describe; each maps to a localized string. */
public enum AppError {
    NETWORK,
    VALIDATION,
    EMAIL_TAKEN,
    INVALID_CREDENTIALS,
    SESSION_EXPIRED,
    FORBIDDEN,
    ICAL_UNREACHABLE,
    ICAL_NOT_A_CALENDAR,
    IMPORT_FAILED,
    STATUS_CONFLICT,
    ALREADY_MEMBER,
    INVITATION_PENDING,
    INVITATION_EXPIRED,
    CANNOT_REMOVE_ADMIN,
    UNKNOWN
}
