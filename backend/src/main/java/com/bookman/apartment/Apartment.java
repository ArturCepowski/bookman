package com.bookman.apartment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "apartment")
public class Apartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "booking_url", nullable = false)
    private String bookingUrl;

    @Column(name = "ical_url", nullable = false)
    private String icalUrl;

    @Column(name = "time_zone", nullable = false)
    private String timeZone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** REQ-11, apartment-local time; null = disabled. ASSUMPTION(REQ-11): off until the admin sets it (Q-23). */
    @Column(name = "not_planned_reminder_time")
    private LocalTime notPlannedReminderTime;

    /** REQ-12, apartment-local time; null = disabled. */
    @Column(name = "not_done_reminder_time")
    private LocalTime notDoneReminderTime;

    protected Apartment() {
    }

    public Apartment(ApartmentData data, Instant createdAt) {
        apply(data);
        this.createdAt = createdAt;
    }

    public void apply(ApartmentData data) {
        this.name = data.name();
        this.bookingUrl = data.bookingUrl();
        this.icalUrl = data.icalUrl();
        this.timeZone = data.timeZone();
    }

    public void setReminderTimes(LocalTime notPlanned, LocalTime notDone) {
        this.notPlannedReminderTime = notPlanned;
        this.notDoneReminderTime = notDone;
    }

    public LocalTime getNotPlannedReminderTime() {
        return notPlannedReminderTime;
    }

    public LocalTime getNotDoneReminderTime() {
        return notDoneReminderTime;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBookingUrl() {
        return bookingUrl;
    }

    public String getIcalUrl() {
        return icalUrl;
    }

    public String getTimeZone() {
        return timeZone;
    }
}
