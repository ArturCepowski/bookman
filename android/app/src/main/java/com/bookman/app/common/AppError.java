package com.bookman.app.common;

/** Failure categories the UI knows how to describe; each maps to a localized string. */
public enum AppError {
    NETWORK,
    VALIDATION,
    EMAIL_TAKEN,
    INVALID_CREDENTIALS,
    SESSION_EXPIRED,
    UNKNOWN
}
