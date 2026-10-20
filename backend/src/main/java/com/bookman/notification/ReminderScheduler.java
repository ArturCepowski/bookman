package com.bookman.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** REQ-11/12 background jobs; reminder hours are checked every minute, the send queue every few seconds. */
@Component
@ConditionalOnProperty(name = "bookman.reminders.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class ReminderScheduler {

    private final ReminderPlanner planner;
    private final ReminderDispatcher dispatcher;

    public ReminderScheduler(ReminderPlanner planner, ReminderDispatcher dispatcher) {
        this.planner = planner;
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${bookman.reminders.planner-interval}")
    public void planReminders() {
        planner.planDue();
    }

    @Scheduled(fixedDelayString = "${bookman.reminders.dispatcher-interval}")
    public void sendQueued() {
        dispatcher.dispatchDue();
    }
}
