package com.bookman.reservation;

import com.bookman.apartment.ApartmentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** REQ-06: re-imports every apartment every 30 minutes ({@code bookman.import.interval}). */
@Component
@ConditionalOnProperty(name = "bookman.import.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class ReservationImportScheduler {

    private final ApartmentRepository apartments;
    private final ImportRunner runner;

    public ReservationImportScheduler(ApartmentRepository apartments, ImportRunner runner) {
        this.apartments = apartments;
        this.runner = runner;
    }

    @Scheduled(fixedDelayString = "${bookman.import.interval}", initialDelayString = "${bookman.import.initial-delay}")
    public void importAll() {
        apartments.findAllIds().forEach(runner::submit);
    }
}
