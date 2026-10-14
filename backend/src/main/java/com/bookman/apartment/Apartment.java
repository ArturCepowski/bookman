package com.bookman.apartment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

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
