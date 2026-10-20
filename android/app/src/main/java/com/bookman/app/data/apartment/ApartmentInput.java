package com.bookman.app.data.apartment;

public final class ApartmentInput {

    public final String name;
    public final String bookingUrl;
    public final String icalUrl;
    public final String timeZone;

    public ApartmentInput(String name, String bookingUrl, String icalUrl, String timeZone) {
        this.name = name;
        this.bookingUrl = bookingUrl;
        this.icalUrl = icalUrl;
        this.timeZone = timeZone;
    }
}
