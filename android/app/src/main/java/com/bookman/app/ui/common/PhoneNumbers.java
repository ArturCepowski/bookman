package com.bookman.app.ui.common;

import java.util.regex.Pattern;

/** Same E.164 rule as the backend (REQ-01, REQ-02). */
public final class PhoneNumbers {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{6,14}$");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\-()]");

    private PhoneNumbers() {
    }

    /** Users type "+48 600 100 200" or "+48-600-100-200"; the backend expects strict E.164. */
    public static String normalize(String phone) {
        return phone == null ? "" : SEPARATORS.matcher(phone).replaceAll("");
    }

    /** Q-24: the number is optional; blank input means "no number" (null). */
    public static String normalizeOptional(String phone) {
        String normalized = normalize(phone);
        return normalized.isEmpty() ? null : normalized;
    }

    /** Valid when absent or strict E.164. */
    public static boolean isValidOptional(String normalized) {
        return normalized == null || isValid(normalized);
    }

    public static boolean isValid(String normalized) {
        return E164.matcher(normalized).matches();
    }
}
