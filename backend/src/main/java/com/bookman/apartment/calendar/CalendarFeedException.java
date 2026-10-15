package com.bookman.apartment.calendar;

public class CalendarFeedException extends Exception {

    public enum Reason {
        UNREACHABLE,
        NOT_A_CALENDAR
    }

    private final Reason reason;

    public CalendarFeedException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
