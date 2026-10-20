package com.bookman.app.data.apartment;

import androidx.annotation.Nullable;

public final class ApartmentDetails {

    public final long id;
    public final String name;
    public final String bookingUrl;
    /** Only sent to admins (NFR-03). */
    @Nullable public final String icalUrl;
    public final String timeZone;
    public final Role role;

    public ApartmentDetails(long id, String name, String bookingUrl, @Nullable String icalUrl, String timeZone,
            Role role) {
        this.id = id;
        this.name = name;
        this.bookingUrl = bookingUrl;
        this.icalUrl = icalUrl;
        this.timeZone = timeZone;
        this.role = role;
    }

    /** UI hint only; the backend enforces REQ-05 independently. */
    public boolean canManage() {
        return role == Role.ADMIN;
    }
}
