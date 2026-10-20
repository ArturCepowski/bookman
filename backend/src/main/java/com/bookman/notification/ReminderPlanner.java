package com.bookman.notification;

import com.bookman.apartment.Apartment;
import com.bookman.apartment.ApartmentMemberRepository;
import com.bookman.apartment.ApartmentRepository;
import com.bookman.reservation.Reservation;
import com.bookman.reservation.ReservationRepository;
import com.bookman.user.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * REQ-11 / REQ-12: at the configured hour (apartment time zone) queues one SMS per recipient and reservation.
 * ASSUMPTION(REQ-11): a reminder is still sent if the backend was down at that exact minute, as long as it comes
 * back within an hour (Q-23); later it is skipped for that day rather than arriving at an odd time.
 */
@Service
public class ReminderPlanner {

    static final Duration GRACE = Duration.ofHours(1);

    private final ApartmentRepository apartments;
    private final ApartmentMemberRepository members;
    private final ReservationRepository reservations;
    private final SmsNotificationRepository notifications;
    private final SmsTexts texts;
    private final Clock clock;

    public ReminderPlanner(ApartmentRepository apartments, ApartmentMemberRepository members,
            ReservationRepository reservations, SmsNotificationRepository notifications, SmsTexts texts, Clock clock) {
        this.apartments = apartments;
        this.members = members;
        this.reservations = reservations;
        this.notifications = notifications;
        this.texts = texts;
        this.clock = clock;
    }

    /** @return number of newly queued SMS */
    @Transactional
    public int planDue() {
        Instant now = clock.instant();
        int queued = 0;
        for (Apartment apartment : apartments.findWithReminders()) {
            queued += plan(apartment, ReminderType.NOT_PLANNED, apartment.getNotPlannedReminderTime(), now);
            queued += plan(apartment, ReminderType.NOT_DONE, apartment.getNotDoneReminderTime(), now);
        }
        return queued;
    }

    private int plan(Apartment apartment, ReminderType type, LocalTime time, Instant now) {
        if (time == null) {
            return 0;
        }
        ZoneId zone = ZoneId.of(apartment.getTimeZone());
        LocalDateTime localNow = LocalDateTime.ofInstant(now, zone);
        int queued = 0;
        // Yesterday too: a 23:30 reminder is still due at 00:10.
        for (LocalDate day : List.of(localNow.toLocalDate(), localNow.toLocalDate().minusDays(1))) {
            LocalDateTime due = day.atTime(time);
            if (!localNow.isBefore(due) && localNow.isBefore(due.plus(GRACE))) {
                queued += queue(apartment, type, day, now);
            }
        }
        return queued;
    }

    private int queue(Apartment apartment, ReminderType type, LocalDate day, Instant now) {
        List<Reservation> due = reservations.findForReminder(apartment.getId(), day.plusDays(type.checkOutInDays),
                type.statuses);
        if (due.isEmpty()) {
            return 0;
        }
        List<User> recipients = members.findMemberUsers(apartment.getId());
        int queued = 0;
        for (Reservation reservation : due) {
            for (User recipient : recipients) {
                String text = texts.reminder(type, apartment.getName(), reservation.getCheckOut(), recipient.getLanguage());
                queued += notifications.insertIfAbsent(reservation.getId(), recipient.getId(), type.name(), day,
                        recipient.getPhoneNumber(), text, now);
            }
        }
        return queued;
    }
}
