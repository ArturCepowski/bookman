package com.bookman.app.ui.common;

import androidx.annotation.StringRes;
import com.bookman.app.R;
import com.bookman.app.data.apartment.Role;

public final class RoleLabels {

    private RoleLabels() {
    }

    @StringRes
    public static int of(Role role) {
        return role == Role.ADMIN ? R.string.role_admin : R.string.role_cleaning_service;
    }
}
