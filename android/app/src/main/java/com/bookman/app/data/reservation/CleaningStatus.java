package com.bookman.app.data.reservation;

public enum CleaningStatus {
    NOT_PLANNED,
    TAKEN,
    DONE,
    /** Dates the owner blocked by hand (Q-20); only the admin sets or changes it. */
    PRIVATE
}
