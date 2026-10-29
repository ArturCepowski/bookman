package com.bookman.app.ui.common;

import android.content.res.ColorStateList;
import android.widget.TextView;
import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import com.bookman.app.R;
import com.bookman.app.data.apartment.Role;
import com.bookman.app.data.reservation.CleaningStatus;

/** Colors of the small labels for cleaning status and role (style Widget.Bookman.Pill). */
public final class Pills {

    private Pills() {
    }

    public static void status(TextView view, CleaningStatus status) {
        view.setText(StatusLabels.of(status));
        switch (status) {
            case TAKEN:
                color(view, R.color.status_taken_bg, R.color.status_taken_fg);
                break;
            case DONE:
                color(view, R.color.status_done_bg, R.color.status_done_fg);
                break;
            case PRIVATE:
                color(view, R.color.status_private_bg, R.color.status_private_fg);
                break;
            default:
                color(view, R.color.status_not_planned_bg, R.color.status_not_planned_fg);
        }
    }

    public static void role(TextView view, Role role) {
        view.setText(RoleLabels.of(role));
        if (role == Role.ADMIN) {
            color(view, R.color.brand_primary, R.color.brand_on_primary);
        } else {
            color(view, R.color.brand_tertiary_container, R.color.brand_on_tertiary_container);
        }
    }

    private static void color(TextView view, @ColorRes int background, @ColorRes int text) {
        view.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), background)));
        view.setTextColor(ContextCompat.getColor(view.getContext(), text));
    }
}
