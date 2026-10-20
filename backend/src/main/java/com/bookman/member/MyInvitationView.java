package com.bookman.member;

import java.time.Instant;

/** REQ-09: an invitation as seen by the invited user. */
public record MyInvitationView(Long id, Long apartmentId, String apartmentName, String invitedBy, Instant expiresAt) {
}
