package com.bookman.reservation;

/** Outcome of one import; {@code skipped} means another import of the same apartment was running. */
public record ImportResult(int added, int updated, int cancelled, boolean skipped) {

    static ImportResult skippedResult() {
        return new ImportResult(0, 0, 0, true);
    }
}
