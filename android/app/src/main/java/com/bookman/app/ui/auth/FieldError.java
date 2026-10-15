package com.bookman.app.ui.auth;

import androidx.annotation.StringRes;
import com.bookman.app.R;

public enum FieldError {
    EMAIL_INVALID(R.string.field_error_email),
    PASSWORD_TOO_SHORT(R.string.field_error_password),
    PHONE_INVALID(R.string.field_error_phone);

    @StringRes public final int message;

    FieldError(@StringRes int message) {
        this.message = message;
    }
}
