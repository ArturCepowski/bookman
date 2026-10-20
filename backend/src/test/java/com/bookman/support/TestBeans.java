package com.bookman.support;

import com.bookman.reservation.ReservationImporter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestBeans {

    @Bean
    @Primary
    FakeCalendarFeedVerifier fakeCalendarFeedVerifier() {
        return new FakeCalendarFeedVerifier();
    }

    @Bean
    @Primary
    FakeReservationSource fakeReservationSource() {
        return new FakeReservationSource();
    }

    @Bean
    @Primary
    FakeSmsSender fakeSmsSender() {
        return new FakeSmsSender();
    }

    @Bean
    @Primary
    MutableClock mutableClock() {
        return new MutableClock();
    }

    @Bean
    @Primary
    SyncImportRunner syncImportRunner(ReservationImporter importer) {
        return new SyncImportRunner(importer);
    }
}
