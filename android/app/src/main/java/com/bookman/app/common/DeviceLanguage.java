package com.bookman.app.common;

import java.util.Locale;
import javax.inject.Inject;

/** REQ-14: Polish if the system language is Polish, otherwise English. */
public class DeviceLanguage {

    private final Locale locale;

    @Inject
    public DeviceLanguage() {
        this(Locale.getDefault());
    }

    public DeviceLanguage(Locale locale) {
        this.locale = locale;
    }

    /** Backend language code: "PL" or "EN". */
    public String code() {
        return "pl".equals(locale.getLanguage()) ? "PL" : "EN";
    }
}
