package com.bookman.app.ui.auth;

import java.util.Locale;
import java.util.regex.Pattern;

/** Client-side checks mirroring the backend rules so users get instant feedback (backend still validates). */
public final class RegistrationForm {

    static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{6,14}$");
    private static final Pattern PHONE_SEPARATORS = Pattern.compile("[\\s\\-()]");

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
        this.emailError = EMAIL.matcher(email).matches() ? null : FieldError.EMAIL_INVALID;
        this.passwordError = password.length() >= MIN_PASSWORD_LENGTH ? null : FieldError.PASSWORD_TOO_SHORT;
        this.phoneError = E164.matcher(phoneNumber).matches() ? null : FieldError.PHONE_INVALID;
    }

    public static RegistrationForm of(String email, String password, String phone) {
        return new RegistrationForm(
                normalizeEmail(email),
                password == null ? "" : password,
                normalizePhone(phone));
    }

    public boolean isValid() {
        return emailError == null && passwordError == null && phoneError == null;
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Users type "+48 600 100 200" or "+48-600-100-200"; the backend expects strict E.164. */
    static String normalizePhone(String phone) {
        return phone == null ? "" : PHONE_SEPARATORS.matcher(phone).replaceAll("");
    }
}
