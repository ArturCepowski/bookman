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
            default:
                return R.string.error_unknown;
        }
    }
}
