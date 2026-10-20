package com.bookman.reservation;

import com.bookman.apartment.ApartmentRegisteredEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** REQ-04: the first import starts right after a new apartment is saved. */
@Component
public class ApartmentRegisteredListener {

    private final ImportRunner runner;

    public ApartmentRegisteredListener(ImportRunner runner) {
        this.runner = runner;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegistered(ApartmentRegisteredEvent event) {
        runner.submit(event.apartmentId());
    }
}
