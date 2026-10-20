package com.bookman.notification;

import com.bookman.sms.SmsSendException;
import com.bookman.sms.SmsSender;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** REQ-13: sends queued SMS through {@link SmsSender}; failures are logged and retried at most 3 times. */
@Service
public class SmsDispatcher {

    private static final Logger log = LoggerFactory.getLogger(SmsDispatcher.class);
    private static final int MAX_PER_RUN = 200;

    private final SmsNotificationRepository notifications;
    private final SmsSender sender;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public SmsDispatcher(SmsNotificationRepository notifications, SmsSender sender, TransactionTemplate transaction,
            Clock clock) {
        this.notifications = notifications;
        this.sender = sender;
        this.transaction = transaction;
        this.clock = clock;
    }

    /** @return number of SMS handled (sent or failed) in this run */
    public int dispatchDue() {
        int handled = 0;
        while (handled < MAX_PER_RUN && Boolean.TRUE.equals(transaction.execute(status -> sendNext()))) {
            handled++;
        }
        return handled;
    }

    private boolean sendNext() {
        Instant now = clock.instant();
        Optional<SmsNotification> next = notifications.lockNextDue(now);
        if (next.isEmpty()) {
            return false;
        }
        SmsNotification sms = next.get();
        try {
            sender.send(sms.getPhoneNumber(), sms.getMessage());
            sms.markSent(now);
        } catch (SmsSendException | RuntimeException e) {
            sms.markFailed(e.getMessage(), now);
            log.warn("SMS {} to user {} failed (attempt {}/{}): {}", sms.getId(), sms.getRecipientUserId(),
                    sms.getAttempts(), SmsNotification.MAX_ATTEMPTS, e.getMessage());
        }
        return true;
    }
}
