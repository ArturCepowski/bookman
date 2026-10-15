package com.bookman.apartment.calendar;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import org.springframework.stereotype.Component;

@Component
public class HttpCalendarFeedVerifier implements CalendarFeedVerifier {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            // Redirects could lead outside the allowed hosts (SSRF), see ApartmentValidator.
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Override
    public void verify(URI uri) throws CalendarFeedException {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build();
        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new CalendarFeedException(CalendarFeedException.Reason.UNREACHABLE, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CalendarFeedException(CalendarFeedException.Reason.UNREACHABLE, "interrupted");
        }
        try (InputStream body = new LimitedInputStream(response.body(), MAX_BYTES)) {
            if (response.statusCode() / 100 != 2) {
                throw new CalendarFeedException(CalendarFeedException.Reason.UNREACHABLE,
                        "HTTP " + response.statusCode());
            }
            new CalendarBuilder().build(new InputStreamReader(body, StandardCharsets.UTF_8));
        } catch (ParserException | IOException e) {
            throw new CalendarFeedException(CalendarFeedException.Reason.NOT_A_CALENDAR, e.getMessage());
        }
    }
}
