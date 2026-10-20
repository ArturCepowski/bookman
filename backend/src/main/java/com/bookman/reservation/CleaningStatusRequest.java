package com.bookman.reservation;

import jakarta.validation.constraints.NotNull;

public record CleaningStatusRequest(@NotNull CleaningStatus status) {
}
