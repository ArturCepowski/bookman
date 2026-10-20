package com.bookman.apartment.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Exercises the real HTTP client against a local server – no external network. */
class CalendarFeedClientTest {

    private HttpServer server;
    private final CalendarFeedClient client = new CalendarFeedClient();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", ex -> respond(ex, 200, fixture()));
        server.createContext("/missing", ex -> respond(ex, 404, "nope".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/html", ex -> respond(ex, 200, "<html>login</html>".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/redirect", ex -> {
            ex.getResponseHeaders().add("Location", "http://169.254.169.254/");
            respond(ex, 302, new byte[0]);
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void REQ04_realBookingCalendarExportIsAccepted() {
        assertThatCode(() -> client.fetch(url("/ok"))).doesNotThrowAnyException();
    }

    @Test
    void REQ04_httpErrorIsUnreachable() {
        assertThat(reasonFor(url("/missing"))).isEqualTo(CalendarFeedException.Reason.UNREACHABLE);
    }

    @Test
    void REQ04_nonCalendarContentIsRejected() {
        assertThat(reasonFor(url("/html"))).isEqualTo(CalendarFeedException.Reason.NOT_A_CALENDAR);
    }

    @Test
    void REQ04_redirectsAreNotFollowed() {
        assertThat(reasonFor(url("/redirect"))).isEqualTo(CalendarFeedException.Reason.UNREACHABLE);
    }

    @Test
    void REQ04_connectionFailureIsUnreachable() {
        URI closed = url("/ok");
        server.stop(0);
        assertThat(reasonFor(closed)).isEqualTo(CalendarFeedException.Reason.UNREACHABLE);
    }

    private CalendarFeedException.Reason reasonFor(URI uri) {
        return catchThrowableOfType(CalendarFeedException.class, () -> client.fetch(uri)).reason();
    }

    private URI url(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    private static byte[] fixture() throws IOException {
        try (InputStream in = CalendarFeedClientTest.class.getResourceAsStream("/ical/booking-export-real.ics")) {
            return in.readAllBytes();
        }
    }

    private static void respond(com.sun.net.httpserver.HttpExchange ex, int status, byte[] body) throws IOException {
        ex.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }
}
