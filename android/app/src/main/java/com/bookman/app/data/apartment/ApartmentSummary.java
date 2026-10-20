package com.bookman.app.data.apartment;

public final class ApartmentSummary {

    public final long id;
    public final String name;
    public final Role role;

    public ApartmentSummary(long id, String name, Role role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }
}
