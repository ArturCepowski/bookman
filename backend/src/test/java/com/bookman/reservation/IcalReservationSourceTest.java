package com.bookman.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookman.apartment.Apartment;
import com.bookman.apartment.ApartmentData;
import com.bookman.apartment.calendar.CalendarFeedClient;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Real HTTP client + parser against a local server serving the real Booking.com export – no external calls. */
class IcalReservationSourceTest {

    private HttpServer server;
    private final IcalReservationSource source = new IcalReservationSource(new CalendarFeedClient(), new IcalReservationParser());

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/export", ex -> {
            byte[] body;
            try (InputStream in = getClass().getResourceAsStream("/ical/booking-export-real.ics")) {
                body = in.readAllBytes();
            }
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream out = ex.getResponseBody()) {
                out.write(body);
            }
        });
        server.createContext("/broken", ex -> {
            ex.sendResponseHeaders(500, -1);
            ex.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void REQ06_downloadsAndParsesTheBookingExport() throws Exception {
        assertThat(source.fetch(apartment("/export"))).containsExactly(new ImportedReservation(
                "5f3c9e1a7b2d4c6e8f0a1b2c3d4e5f60@booking.com", LocalDate.of(2026, 10, 22), LocalDate.of(2026, 10, 25)));
    }

    @Test
    void REQ06_unreadableFeedFailsInsteadOfReturningEmpty() {
        assertThatThrownBy(() -> source.fetch(apartment("/broken"))).isInstanceOf(ReservationSourceException.class);
    }

    private Apartment apartment(String path) {
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + path;
        return new Apartment(new ApartmentData("Flat", "https://www.booking.com/h", url, "Europe/Warsaw"), Instant.now());
    }
}
