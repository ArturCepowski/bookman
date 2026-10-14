package com.bookman.app.data.api;

public class ApartmentRequestDto {
    public final String name;
    public final String bookingUrl;
    public final String icalUrl;
    public final String timeZone;

    public ApartmentRequestDto(String name, String bookingUrl, String icalUrl, String timeZone) {
        this.name = name;
        this.bookingUrl = bookingUrl;
        this.icalUrl = icalUrl;
        this.timeZone = timeZone;
    }
}
