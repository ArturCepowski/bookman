package com.bookman.app.ui.common;

import androidx.annotation.StringRes;
import com.bookman.app.R;
import com.bookman.app.data.reservation.CleaningStatus;

public final class StatusLabels {

    private StatusLabels() {
    }

    @StringRes
    public static int of(CleaningStatus status) {
        switch (status) {
            case TAKEN:
                return R.string.status_taken;
            case DONE:
                return R.string.status_done;
            default:
                return R.string.status_not_planned;
        }
    }

    /** Label of the button that moves a reservation to {@code target}. */
    @StringRes
    public static int actionTo(CleaningStatus target) {
        switch (target) {
            case TAKEN:
                return R.string.action_take_cleaning;
            case DONE:
                return R.string.action_mark_cleaned;
            default:
                return R.string.action_mark_not_planned;
        }
    }
}
