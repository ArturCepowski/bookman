package com.bookman.app.data.member;

import androidx.annotation.Nullable;
import java.time.Instant;

/** REQ-09: an invitation addressed to the logged-in user. */
public final class MyInvitation {

    public final long id;
    public final long apartmentId;
    public final String apartmentName;
    @Nullable public final String invitedBy;
    public final Instant expiresAt;

    public MyInvitation(long id, long apartmentId, String apartmentName, @Nullable String invitedBy,
            Instant expiresAt) {
        this.id = id;
        this.apartmentId = apartmentId;
        this.apartmentName = apartmentName;
        this.invitedBy = invitedBy;
        this.expiresAt = expiresAt;
    }
}
