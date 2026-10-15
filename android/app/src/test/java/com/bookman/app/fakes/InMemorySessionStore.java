package com.bookman.app.fakes;

import com.bookman.app.data.session.SessionStore;

public class InMemorySessionStore implements SessionStore {

    private String token;

    public InMemorySessionStore() {
    }

    public InMemorySessionStore(String token) {
        this.token = token;
    }

    @Override
    public String token() {
        return token;
    }

    @Override
    public void save(String token) {
        this.token = token;
    }

    @Override
    public void clear() {
        token = null;
    }
}
