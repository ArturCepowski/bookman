package com.bookman.reservation;

import com.bookman.apartment.Role;
import com.bookman.common.ApiException;
import com.bookman.common.ErrorCode;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;

/**
 * REQ-08 / Q-02: CLEANING_SERVICE may only go NOT_PLANNED → TAKEN → DONE; ADMIN may set any status.
 * ASSUMPTION(REQ-08): any cleaning service member may finish a cleaning a colleague took (Q-21).
 */
final class CleaningStatusPolicy {

    private static final Map<CleaningStatus, Set<CleaningStatus>> CLEANING_SERVICE_MOVES = Map.of(
            CleaningStatus.NOT_PLANNED, Set.of(CleaningStatus.TAKEN),
            CleaningStatus.TAKEN, Set.of(CleaningStatus.DONE),
            CleaningStatus.DONE, Set.of());

    private CleaningStatusPolicy() {
    }

    static void check(Role role, CleaningStatus current, CleaningStatus requested) {
        if (current == requested) {
            // Typically someone else changed it first and this screen is stale.
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.STATUS_CONFLICT, "Status is already " + current);
        }
        if (role == Role.ADMIN) {
            return;
        }
        if (!CLEANING_SERVICE_MOVES.get(current).contains(requested)) {
            throw ApiException.forbidden();
        }
    }
}
