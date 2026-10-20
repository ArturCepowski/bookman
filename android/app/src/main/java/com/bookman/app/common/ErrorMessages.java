package com.bookman.app.common;

import androidx.annotation.StringRes;
import com.bookman.app.R;

public final class ErrorMessages {

    private ErrorMessages() {
    }

    @StringRes
    public static int of(AppError error) {
        switch (error) {
            case NETWORK:
                return R.string.error_network;
            case VALIDATION:
                return R.string.error_validation;
            case EMAIL_TAKEN:
                return R.string.error_email_taken;
            case INVALID_CREDENTIALS:
                return R.string.error_invalid_credentials;
            case SESSION_EXPIRED:
                return R.string.error_session_expired;
            case FORBIDDEN:
                return R.string.error_forbidden;
            case ICAL_UNREACHABLE:
                return R.string.error_ical_unreachable;
            case ICAL_NOT_A_CALENDAR:
                return R.string.error_ical_not_a_calendar;
            case IMPORT_FAILED:
                return R.string.error_import_failed;
            case STATUS_CONFLICT:
                return R.string.error_status_conflict;
            default:
                return R.string.error_unknown;
        }
    }
}
