package com.bookman.app.common;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import org.junit.Test;

public class DeviceLanguageTest {

    @Test
    public void REQ14_polishSystemGivesPolishOtherwiseEnglish() {
        assertEquals("PL", new DeviceLanguage(new Locale("pl", "PL")).code());
        assertEquals("EN", new DeviceLanguage(Locale.GERMANY).code());
        assertEquals("EN", new DeviceLanguage(Locale.US).code());
    }
}
