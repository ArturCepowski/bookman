package com.bookman.app.ui.auth;

import com.bookman.app.ui.common.EmailAddresses;
import com.bookman.app.ui.common.FieldError;
import com.bookman.app.ui.common.PhoneNumbers;


/** Client-side checks mirroring the backend rules so users get instant feedback (backend still validates). */
public final class RegistrationForm {

    static final int MIN_PASSWORD_LENGTH = 8;

    public final String email;
    public final String password;
    public final String phoneNumber;
    public final FieldError emailError;
    public final FieldError passwordError;
    public final FieldError phoneError;

    private RegistrationForm(String email, String password, String phoneNumber) {
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.emailError = EmailAddresses.isValid(email) ? null : FieldError.EMAIL_INVALID;
        this.passwordError = password.length() >= MIN_PASSWORD_LENGTH ? null : FieldError.PASSWORD_TOO_SHORT;
        this.phoneError = PhoneNumbers.isValid(phoneNumber) ? null : FieldError.PHONE_INVALID;
    }

    public static RegistrationForm of(String email, String password, String phone) {
        return new RegistrationForm(
                normalizeEmail(email),
                password == null ? "" : password,
                PhoneNumbers.normalize(phone));
    }

    public boolean isValid() {
        return emailError == null && passwordError == null && phoneError == null;
    }

    static String normalizeEmail(String email) {
        return EmailAddresses.normalize(email);
    }
}
