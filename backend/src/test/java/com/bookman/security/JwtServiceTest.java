package com.bookman.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789abcdef0123456789";
    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void REQ01_tokenIsValidUntilTtlThenExpires() {
        JwtProperties props = new JwtProperties(SECRET, Duration.ofDays(30));
        String token = new JwtService(props, Clock.fixed(T0, ZoneOffset.UTC)).issueToken(42L);

        assertThat(new JwtService(props, Clock.fixed(T0.plus(Duration.ofDays(29)), ZoneOffset.UTC))
                .parseUserId(token)).contains(42L);
        assertThat(new JwtService(props, Clock.fixed(T0.plus(Duration.ofDays(31)), ZoneOffset.UTC))
                .parseUserId(token)).isEmpty();
    }

    @Test
    void REQ01_tokenSignedWithDifferentKeyIsRejected() {
        Clock clock = Clock.fixed(T0, ZoneOffset.UTC);
        String foreign = new JwtService(new JwtProperties(SECRET.replace('u', 'x'), Duration.ofHours(1)), clock)
                .issueToken(1L);

        assertThat(new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)), clock).parseUserId(foreign))
                .isEmpty();
    }

    @Test
    void missingOrShortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("", Duration.ofHours(1)), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }
}
