package com.bookman.auth;

public final class PhoneNumbers {

    /** E.164: '+', country code not starting with 0, at most 15 digits in total. */
    public static final String E164 = "^\\+[1-9]\\d{6,14}$";

    private PhoneNumbers() {
    }
}
