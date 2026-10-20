package com.bookman.notification;

import com.bookman.user.Language;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/** REQ-11/12: SMS contains apartment name and check-out date; REQ-14: in the recipient's language. */
@Component
public class SmsTexts {

    private final MessageSource messages;

    public SmsTexts(MessageSource messages) {
        this.messages = messages;
    }

    public String reminder(ReminderType type, String apartmentName, LocalDate checkOut, Language language) {
        Locale locale = language == Language.PL ? Locale.forLanguageTag("pl") : Locale.ENGLISH;
        String date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(checkOut);
        return messages.getMessage(type.messageKey, new Object[] {apartmentName, date}, locale);
    }
}
