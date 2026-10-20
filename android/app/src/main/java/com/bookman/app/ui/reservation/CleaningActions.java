package com.bookman.app.ui.reservation;

import com.bookman.app.data.apartment.Role;
import com.bookman.app.data.reservation.CleaningStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Which status changes to offer (REQ-08 / Q-02); PRIVATE is admin-only and frozen for the cleaning service (Q-20).
 * UI hint only – the backend enforces the same rules.
 */
public final class CleaningActions {

    private CleaningActions() {
    }

    public static List<CleaningStatus> targets(Role role, CleaningStatus current) {
        if (role == Role.ADMIN) {
            List<CleaningStatus> all = new ArrayList<>();
            for (CleaningStatus status : CleaningStatus.values()) {
                if (status != current) {
                    all.add(status);
                }
            }
            return all;
        }
        switch (current) {
            case NOT_PLANNED:
                return Collections.singletonList(CleaningStatus.TAKEN);
            case TAKEN:
                return Collections.singletonList(CleaningStatus.DONE);
            default:
                return Collections.emptyList();
        }
    }
}
