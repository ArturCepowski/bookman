package com.bookman.support;

import com.bookman.apartment.calendar.CalendarFeedException;
import com.bookman.apartment.calendar.CalendarFeedVerifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Never touches the network (CLAUDE.md: no external calls from tests). */
public class FakeCalendarFeedVerifier implements CalendarFeedVerifier {

    private final Map<String, CalendarFeedException.Reason> failures = new HashMap<>();
    private final List<URI> verified = new ArrayList<>();

    public void failFor(String url, CalendarFeedException.Reason reason) {
        failures.put(url, reason);
    }

    public List<URI> verified() {
        return verified;
    }

    public void reset() {
        failures.clear();
        verified.clear();
    }

    @Override
    public void verify(URI uri) throws CalendarFeedException {
        verified.add(uri);
        CalendarFeedException.Reason reason = failures.get(uri.toString());
        if (reason != null) {
            throw new CalendarFeedException(reason, "fake");
        }
    }
}
