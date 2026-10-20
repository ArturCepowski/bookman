package com.bookman.apartment.calendar;

import java.net.URI;
import org.springframework.stereotype.Component;

@Component
public class HttpCalendarFeedVerifier implements CalendarFeedVerifier {

    private final CalendarFeedClient client;

    public HttpCalendarFeedVerifier(CalendarFeedClient client) {
        this.client = client;
    }

    @Override
    public void verify(URI uri) throws CalendarFeedException {
        client.fetch(uri);
    }
}
