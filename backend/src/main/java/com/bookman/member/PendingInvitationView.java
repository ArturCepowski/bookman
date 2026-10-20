package com.bookman.member;

import java.time.Instant;

/** REQ-10: an invitation as seen by the apartment's admin. */
public record PendingInvitationView(Long id, String email, Instant expiresAt) {

    static PendingInvitationView of(Invitation invitation) {
        return new PendingInvitationView(invitation.getId(), invitation.getEmail(), invitation.getExpiresAt());
    }
}
