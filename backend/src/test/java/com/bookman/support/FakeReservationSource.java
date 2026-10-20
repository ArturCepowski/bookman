package com.bookman.support;

import com.bookman.apartment.Apartment;
import com.bookman.reservation.ImportedReservation;
import com.bookman.reservation.ReservationSource;
import com.bookman.reservation.ReservationSourceException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Feeds keyed by iCal URL; never touches Booking.com (CLAUDE.md). */
public class FakeReservationSource implements ReservationSource {

    private final Map<String, List<ImportedReservation>> feeds = new HashMap<>();
    private final Set<String> failing = new HashSet<>();
    private int fetches;

    public void feed(String icalUrl, ImportedReservation... reservations) {
        feeds.put(icalUrl, List.of(reservations));
        failing.remove(icalUrl);
    }

    public void fail(String icalUrl) {
        failing.add(icalUrl);
    }

    public int fetches() {
        return fetches;
    }

    public void reset() {
        feeds.clear();
        failing.clear();
        fetches = 0;
    }

    @Override
    public List<ImportedReservation> fetch(Apartment apartment) throws ReservationSourceException {
        fetches++;
        if (failing.contains(apartment.getIcalUrl())) {
            throw new ReservationSourceException("fake failure");
        }
        return new ArrayList<>(feeds.getOrDefault(apartment.getIcalUrl(), List.of()));
    }
}
