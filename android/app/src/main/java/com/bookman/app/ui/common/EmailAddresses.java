package com.bookman.app.ui.common;

import java.util.Locale;
import java.util.regex.Pattern;

/** Same normalization as the backend (Q-14): trimmed, lower-case. */
public final class EmailAddresses {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private EmailAddresses() {
    }

    public static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String normalized) {
        return EMAIL.matcher(normalized).matches();
    }
}
