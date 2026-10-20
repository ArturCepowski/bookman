package com.bookman.app.data.member;

import java.util.Collections;
import java.util.List;

public final class MembersOverview {

    public final List<Member> members;
    public final List<PendingInvitation> invitations;

    public MembersOverview(List<Member> members, List<PendingInvitation> invitations) {
        this.members = Collections.unmodifiableList(members);
        this.invitations = Collections.unmodifiableList(invitations);
    }

    public static MembersOverview empty() {
        return new MembersOverview(Collections.emptyList(), Collections.emptyList());
    }
}
