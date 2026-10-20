package com.bookman.reservation;

public enum CleaningStatus {
    NOT_PLANNED,
    TAKEN,
    DONE,
    /** Q-20: dates the owner blocked by hand, no cleaning needed; set and changed by ADMIN only. */
    PRIVATE
}
