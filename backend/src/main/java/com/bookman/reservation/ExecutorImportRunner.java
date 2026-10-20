package com.bookman.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class ExecutorImportRunner implements ImportRunner {

    private static final Logger log = LoggerFactory.getLogger(ExecutorImportRunner.class);

    private final TaskExecutor executor;
    private final ReservationImporter importer;

    public ExecutorImportRunner(@Qualifier(ImportConfig.EXECUTOR) TaskExecutor executor, ReservationImporter importer) {
        this.executor = executor;
        this.importer = importer;
    }

    @Override
    public void submit(long apartmentId) {
        executor.execute(() -> run(importer, apartmentId));
    }

    static void run(ReservationImporter importer, long apartmentId) {
        try {
            ImportResult result = importer.importApartment(apartmentId);
            log.info("Import of apartment {}: {}", apartmentId, result);
        } catch (ReservationSourceException e) {
            // Retried on the next scheduled run; nothing was changed.
            log.warn("Import of apartment {} failed: {}", apartmentId, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Import of apartment {} failed", apartmentId, e);
        }
    }
}
