package com.bookman.app.data.auth;

public final class User {

    private final long id;
    private final String email;
    private final String phoneNumber;
    private final String language;

    public User(long id, String email, String phoneNumber, String language) {
        this.id = id;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.language = language;
    }

    public long id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public String language() {
        return language;
    }
}
