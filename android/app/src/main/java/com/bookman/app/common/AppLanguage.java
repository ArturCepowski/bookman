package com.bookman.app.common;

/**
 * REQ-14: the app UI language. Before login it follows the system language (resource fallback); after login and
 * after a change in the profile it follows the language stored in the user profile.
 * ASSUMPTION(REQ-14): the language is synced from the profile at login/registration and on a change in the
 * profile screen (not on every start), and it stays as it is after logout (Q-26).
 */
public interface AppLanguage {

    /** @param code backend language code, "PL" or "EN"; the change applies immediately (activities are recreated) */
    void apply(String code);

    /** BCP 47 tag for a backend language code: "pl" for Polish, otherwise "en". */
    static String tagFor(String code) {
        return "PL".equals(code) ? "pl" : "en";
    }
}
