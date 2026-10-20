package com.bookman.apartment.calendar;

import java.net.URI;

/** Checks that a calendar export URL can be downloaded and parsed (REQ-04, Q-12). */
public interface CalendarFeedVerifier {

    void verify(URI uri) throws CalendarFeedException;
}
