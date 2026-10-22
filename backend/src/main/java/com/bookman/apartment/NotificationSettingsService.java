package com.bookman.apartment;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** REQ-05: notification settings are part of the apartment data and editable by its ADMIN only. */
@Service
public class NotificationSettingsService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final ApartmentAccess access;
    private final ApartmentRepository apartments;

    public NotificationSettingsService(ApartmentAccess access, ApartmentRepository apartments) {
        this.access = access;
        this.apartments = apartments;
    }

    @Transactional(readOnly = true)
    public NotificationSettings get(long apartmentId, long userId) {
        access.requireAdmin(apartmentId, userId);
        return toSettings(apartments.getReferenceById(apartmentId));
    }

    @Transactional
    public NotificationSettings update(long apartmentId, long userId, NotificationSettings settings) {
        access.requireAdmin(apartmentId, userId);
        Apartment apartment = apartments.getReferenceById(apartmentId);
        apartment.setReminderTimes(parse(settings.notPlannedTime()), parse(settings.notDoneTime()));
        return toSettings(apartment);
    }

    private static NotificationSettings toSettings(Apartment a) {
        return new NotificationSettings(format(a.getNotPlannedReminderTime()), format(a.getNotDoneReminderTime()));
    }

    private static LocalTime parse(String value) {
        return value == null ? null : LocalTime.parse(value, HH_MM);
    }

    private static String format(LocalTime value) {
        return value == null ? null : HH_MM.format(value);
    }
}
