package com.bookman.app.data.member;

import java.time.Instant;

public final class PendingInvitation {

    public final long id;
    public final String email;
    public final Instant expiresAt;

    public PendingInvitation(long id, String email, Instant expiresAt) {
        this.id = id;
        this.email = email;
        this.expiresAt = expiresAt;
    }
}
