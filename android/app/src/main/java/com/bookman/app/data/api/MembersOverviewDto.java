package com.bookman.app.data.api;

import java.util.List;

public class MembersOverviewDto {
    public List<MemberDto> members;
    public List<PendingInvitationDto> invitations;

    public static class MemberDto {
        public long userId;
        public String email;
        public String role;
    }
}
