package com.bookman.apartment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ApartmentUrlPolicyTest {

    private final ApartmentUrlPolicy policy = new ApartmentUrlPolicy(List.of());

    @Test
    void REQ04_acceptsHttpsBookingComUrls() {
        assertThat(policy.parseAllowed("https://ical.booking.com/v1/export?t=abc")).isPresent();
        assertThat(policy.parseAllowed(" https://www.booking.com/hotel/pl/flat.html ")).isPresent();
        assertThat(policy.parseAllowed("https://booking.com/x")).isPresent();
    }

    @Test
    void REQ04_rejectsUrlsThatCouldReachOtherHosts() {
        assertThat(policy.parseAllowed("http://ical.booking.com/v1/export")).isEmpty();
        assertThat(policy.parseAllowed("https://booking.com.evil.example/x")).isEmpty();
        assertThat(policy.parseAllowed("https://evilbooking.com/x")).isEmpty();
        assertThat(policy.parseAllowed("https://169.254.169.254/latest/meta-data")).isEmpty();
        assertThat(policy.parseAllowed("https://user:pw@booking.com/x")).isEmpty();
        assertThat(policy.parseAllowed("https://booking.com:8443/x")).isEmpty();
        assertThat(policy.parseAllowed("file:///etc/passwd")).isEmpty();
        assertThat(policy.parseAllowed("not a url")).isEmpty();
    }
}
