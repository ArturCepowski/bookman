package com.bookman.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** REQ-11/12/13 background jobs; reminder hours are checked every minute, the send queue every few seconds. */
@Component
@ConditionalOnProperty(name = "bookman.sms.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class ReminderScheduler {

    private final ReminderPlanner planner;
    private final SmsDispatcher dispatcher;

    public ReminderScheduler(ReminderPlanner planner, SmsDispatcher dispatcher) {
        this.planner = planner;
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${bookman.sms.planner-interval}")
    public void planReminders() {
        planner.planDue();
    }

    @Scheduled(fixedDelayString = "${bookman.sms.dispatcher-interval}")
    public void sendQueued() {
        dispatcher.dispatchDue();
    }
}
