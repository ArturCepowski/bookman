package com.bookman.apartment;

import jakarta.validation.constraints.Pattern;

/**
 * REQ-11 / REQ-12 reminder hours as "HH:MM" in the apartment's time zone; null means the reminder is disabled.
 */
public record NotificationSettings(
        @Pattern(regexp = HH_MM, message = "must be HH:MM") String notPlannedTime,
        @Pattern(regexp = HH_MM, message = "must be HH:MM") String notDoneTime) {

    static final String HH_MM = "^([01]\\d|2[0-3]):[0-5]\\d$";
}
