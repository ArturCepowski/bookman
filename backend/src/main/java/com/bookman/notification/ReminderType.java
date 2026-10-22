package com.bookman.notification;

import com.bookman.reservation.CleaningStatus;
import java.util.EnumSet;
import java.util.Set;

public enum ReminderType {
    /** REQ-11: check-out tomorrow, still NOT_PLANNED. */
    NOT_PLANNED(1, EnumSet.of(CleaningStatus.NOT_PLANNED), "reminder.not-planned"),
    /** REQ-12: check-out today, not DONE. PRIVATE is excluded – no cleaning is needed (Q-20). */
    NOT_DONE(0, EnumSet.of(CleaningStatus.NOT_PLANNED, CleaningStatus.TAKEN), "reminder.not-done");

    final int checkOutInDays;
    final Set<CleaningStatus> statuses;
    final String messageKey;

    ReminderType(int checkOutInDays, Set<CleaningStatus> statuses, String messageKey) {
        this.checkOutInDays = checkOutInDays;
        this.statuses = statuses;
        this.messageKey = messageKey;
    }
}
