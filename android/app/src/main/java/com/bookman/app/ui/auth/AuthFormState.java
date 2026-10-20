package com.bookman.app.ui.auth;

import com.bookman.app.ui.common.FieldError;

import androidx.annotation.Nullable;

/** Immutable snapshot of a login/registration screen. */
public final class AuthFormState {

    public final boolean loading;
    @Nullable public final FieldError emailError;
    @Nullable public final FieldError passwordError;
    @Nullable public final FieldError phoneError;

    public AuthFormState(boolean loading, @Nullable FieldError emailError, @Nullable FieldError passwordError,
            @Nullable FieldError phoneError) {
        this.loading = loading;
        this.emailError = emailError;
        this.passwordError = passwordError;
        this.phoneError = phoneError;
    }

    public static AuthFormState idle() {
        return new AuthFormState(false, null, null, null);
    }

    public static AuthFormState loading() {
        return new AuthFormState(true, null, null, null);
    }
}
