package com.bookman.app.ui.reservation;

import androidx.annotation.Nullable;
import com.bookman.app.data.apartment.Role;
import com.bookman.app.data.reservation.CleaningStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Which status changes to offer (REQ-08 / Q-02); PRIVATE is admin-only and frozen for the cleaning service (Q-20);
 * the cleaning service may undo a status it set itself (Q-28).
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

    /**
     * Q-28: the step back the cleaning service may take on a status it set itself (TAKEN → NOT_PLANNED,
     * DONE → TAKEN), or null. The admin chooses any status through {@link #targets} instead.
     */
    @Nullable
    public static CleaningStatus undo(Role role, CleaningStatus current, boolean changedByMe) {
        if (role == Role.ADMIN || !changedByMe) {
            return null;
        }
        switch (current) {
            case TAKEN:
                return CleaningStatus.NOT_PLANNED;
            case DONE:
                return CleaningStatus.TAKEN;
            default:
                return null;
        }
    }
}
