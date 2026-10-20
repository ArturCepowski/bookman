package com.bookman.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Real time by default; tests that care about "now" (reminder hours) set it explicitly. */
public class MutableClock extends Clock {

    private volatile Instant fixed;

    public void set(Instant instant) {
        this.fixed = instant;
    }

    public void reset() {
        this.fixed = null;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return fixed == null ? Clock.system(zone) : Clock.fixed(fixed, zone);
    }

    @Override
    public Instant instant() {
        return fixed == null ? Instant.now() : fixed;
    }
}
