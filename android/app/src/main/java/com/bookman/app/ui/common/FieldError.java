package com.bookman.app.ui.common;

import androidx.annotation.StringRes;
import com.bookman.app.R;

public enum FieldError {
    REQUIRED(R.string.field_error_required),
    EMAIL_INVALID(R.string.field_error_email),
    PASSWORD_TOO_SHORT(R.string.field_error_password),
    PHONE_INVALID(R.string.field_error_phone),
    NAME_INVALID(R.string.field_error_apartment_name),
    BOOKING_URL_INVALID(R.string.field_error_booking_url),
    ICAL_URL_INVALID(R.string.field_error_ical_url),
    TIME_ZONE_INVALID(R.string.field_error_time_zone);

    @StringRes public final int message;

    FieldError(@StringRes int message) {
        this.message = message;
    }
}
