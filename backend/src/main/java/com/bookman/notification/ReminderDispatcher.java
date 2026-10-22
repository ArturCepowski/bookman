package com.bookman.notification;

import com.bookman.device.DeviceService;
import com.bookman.push.PushSendException;
import com.bookman.push.PushSender;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Sends queued reminders through {@link PushSender} to every phone of the recipient (Q-24). Failures are logged
 * and retried at most 3 times; tokens that FCM reports as invalid are forgotten.
 */
@Service
public class ReminderDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ReminderDispatcher.class);
    private static final int MAX_PER_RUN = 200;

    private final ReminderNotificationRepository notifications;
    private final DeviceService devices;
    private final PushSender sender;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public ReminderDispatcher(ReminderNotificationRepository notifications, DeviceService devices, PushSender sender,
            TransactionTemplate transaction, Clock clock) {
        this.notifications = notifications;
        this.devices = devices;
        this.sender = sender;
        this.transaction = transaction;
        this.clock = clock;
    }

    /** @return number of reminders handled in this run */
    public int dispatchDue() {
        int handled = 0;
        while (handled < MAX_PER_RUN && Boolean.TRUE.equals(transaction.execute(status -> sendNext()))) {
            handled++;
        }
        return handled;
    }

    private boolean sendNext() {
        Instant now = clock.instant();
        Optional<ReminderNotification> next = notifications.lockNextDue(now);
        if (next.isEmpty()) {
            return false;
        }
        ReminderNotification reminder = next.get();
        List<String> tokens = devices.tokensOf(reminder.getRecipientUserId());
        int delivered = 0;
        String lastError = null;
        for (String token : tokens) {
            try {
                sender.send(token, reminder.getTitle(), reminder.getBody());
                delivered++;
            } catch (PushSendException e) {
                if (e.isInvalidToken()) {
                    devices.forget(token);
                } else {
                    lastError = e.getMessage();
                }
            } catch (RuntimeException e) {
                lastError = e.getMessage();
            }
        }
        if (delivered > 0) {
            reminder.markSent(now);
        } else if (lastError != null) {
            reminder.markFailed(lastError, now);
            log.warn("Reminder {} to user {} failed (attempt {}/{}): {}", reminder.getId(),
                    reminder.getRecipientUserId(), reminder.getAttempts(), ReminderNotification.MAX_ATTEMPTS, lastError);
        } else {
            reminder.markNoDevice(now);
        }
        return true;
    }
}
