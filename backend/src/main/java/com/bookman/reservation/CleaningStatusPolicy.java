package com.bookman.reservation;

import com.bookman.apartment.Role;
import com.bookman.common.ApiException;
import com.bookman.common.ErrorCode;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;

/**
 * REQ-08 / Q-02: CLEANING_SERVICE may only go NOT_PLANNED → TAKEN → DONE; ADMIN may set any status.
 * Q-21 (owner decision): any cleaning service member may finish a cleaning a colleague took.
 * Q-20 (owner decision): PRIVATE is set by ADMIN only, and CLEANING_SERVICE cannot change it.
 * Q-28 (owner decision, replaces "cannot revert" from Q-02): CLEANING_SERVICE may undo a status it set itself –
 * TAKEN → NOT_PLANNED and DONE → TAKEN.
 */
final class CleaningStatusPolicy {

    private static final Map<CleaningStatus, Set<CleaningStatus>> CLEANING_SERVICE_MOVES = Map.of(
            CleaningStatus.NOT_PLANNED, Set.of(CleaningStatus.TAKEN),
            CleaningStatus.TAKEN, Set.of(CleaningStatus.DONE),
            CleaningStatus.DONE, Set.of(),
            CleaningStatus.PRIVATE, Set.of());

    private static final Map<CleaningStatus, CleaningStatus> UNDO = Map.of(
            CleaningStatus.TAKEN, CleaningStatus.NOT_PLANNED,
            CleaningStatus.DONE, CleaningStatus.TAKEN);

    private CleaningStatusPolicy() {
    }

    /** @param currentSetByActor whether the current status was set by the user asking for the change */
    static void check(Role role, CleaningStatus current, CleaningStatus requested, boolean currentSetByActor) {
        if (current == requested) {
            // Typically someone else changed it first and this screen is stale.
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.STATUS_CONFLICT, "Status is already " + current);
        }
        if (role == Role.ADMIN) {
            return;
        }
        if (currentSetByActor && requested == UNDO.get(current)) {
            return;
        }
        if (!CLEANING_SERVICE_MOVES.get(current).contains(requested)) {
            throw ApiException.forbidden();
        }
    }
}
