package com.bookman.support;

import com.bookman.reservation.ImportRunner;
import com.bookman.reservation.ReservationImporter;
import com.bookman.reservation.ReservationSourceException;

/** Runs imports on the calling thread so tests can assert on the result immediately. */
public class SyncImportRunner implements ImportRunner {

    private final ReservationImporter importer;

    public SyncImportRunner(ReservationImporter importer) {
        this.importer = importer;
    }

    @Override
    public void submit(long apartmentId) {
        try {
            importer.importApartment(apartmentId);
        } catch (ReservationSourceException e) {
            // Same as production: a failed background import changes nothing.
        }
    }
}
