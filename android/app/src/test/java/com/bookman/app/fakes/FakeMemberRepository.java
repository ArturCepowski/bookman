package com.bookman.app.fakes;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;
import com.bookman.app.data.apartment.ApartmentSummary;
import com.bookman.app.data.apartment.Role;
import com.bookman.app.data.member.MemberRepository;
import com.bookman.app.data.member.MembersOverview;
import com.bookman.app.data.member.MyInvitation;
import com.bookman.app.data.member.PendingInvitation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class FakeMemberRepository implements MemberRepository {

    public Result<MembersOverview> overviewResult = Result.success(MembersOverview.empty());
    public Result<PendingInvitation> inviteResult =
            Result.success(new PendingInvitation(9, "cleaner@example.com", Instant.parse("2026-11-03T10:00:00Z")));
    public Result<Boolean> changeResult = Result.success(true);
    public Result<List<MyInvitation>> myInvitationsResult = Result.success(new ArrayList<>());
    public Result<ApartmentSummary> acceptResult = Result.success(new ApartmentSummary(5, "Sunny flat", Role.CLEANING_SERVICE));
    public final List<String> calls = new ArrayList<>();
    public String lastInvitedEmail;

    public static MyInvitation invitation(long id) {
        return new MyInvitation(id, 5, "Sunny flat", "admin@example.com", Instant.parse("2026-11-03T10:00:00Z"));
    }

    @Override
    public void overview(long apartmentId, Consumer<Result<MembersOverview>> callback) {
        calls.add("overview");
        callback.accept(overviewResult);
    }

    @Override
    public void invite(long apartmentId, String email, Consumer<Result<PendingInvitation>> callback) {
        calls.add("invite");
        lastInvitedEmail = email;
        callback.accept(inviteResult);
    }

    @Override
    public void cancelInvitation(long apartmentId, long invitationId, Consumer<Result<Boolean>> callback) {
        calls.add("cancel:" + invitationId);
        callback.accept(changeResult);
    }

    @Override
    public void removeMember(long apartmentId, long userId, Consumer<Result<Boolean>> callback) {
        calls.add("remove:" + userId);
        callback.accept(changeResult);
    }

    @Override
    public void myInvitations(Consumer<Result<List<MyInvitation>>> callback) {
        calls.add("mine");
        callback.accept(myInvitationsResult);
    }

    @Override
    public void accept(long invitationId, Consumer<Result<ApartmentSummary>> callback) {
        calls.add("accept:" + invitationId);
        callback.accept(acceptResult);
    }

    @Override
    public void decline(long invitationId, Consumer<Result<Boolean>> callback) {
        calls.add("decline:" + invitationId);
        callback.accept(changeResult);
    }
}
