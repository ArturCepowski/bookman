package com.bookman.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookman.user.Language;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

/** REQ-14: reminder texts in the recipient's profile language. */
class ReminderTextsTest {

    private final ReminderTexts texts = new ReminderTexts(messages());

    @Test
    void REQ14_reminderTextsInEnglishAndPolish() {
        LocalDate checkOut = LocalDate.of(2026, 11, 11);

        assertThat(texts.title(Language.EN)).isEqualTo("Bookman – cleaning reminder");
        assertThat(texts.body(ReminderType.NOT_PLANNED, "Sunny flat", checkOut, Language.EN))
                .isEqualTo("Cleaning is not planned yet for Sunny flat, check-out Nov 11, 2026.");

        assertThat(texts.title(Language.PL)).isEqualTo("Bookman – przypomnienie o sprzątaniu");
        assertThat(texts.body(ReminderType.NOT_DONE, "Sunny flat", checkOut, Language.PL))
                .isEqualTo("Sprzątanie nie zostało jeszcze wykonane – Sunny flat, wyjazd dziś (11 lis 2026).");
    }

    private static ResourceBundleMessageSource messages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("reminders");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
