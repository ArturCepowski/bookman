package com.bookman.member;

import com.bookman.apartment.MemberView;
import java.util.List;

/** REQ-10: users with a role in the apartment and pending invitations. */
public record MembersOverview(List<MemberView> members, List<PendingInvitationView> invitations) {
}
