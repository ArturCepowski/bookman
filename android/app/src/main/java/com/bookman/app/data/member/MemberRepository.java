package com.bookman.app.data.member;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;
import com.bookman.app.data.apartment.ApartmentSummary;
import java.util.List;

public interface MemberRepository {

    /** REQ-10 (admin) */
    void overview(long apartmentId, Consumer<Result<MembersOverview>> callback);

    /** REQ-09 (admin) */
    void invite(long apartmentId, String email, Consumer<Result<PendingInvitation>> callback);

    /** REQ-10 (admin) */
    void cancelInvitation(long apartmentId, long invitationId, Consumer<Result<Boolean>> callback);

    /** REQ-10 (admin) */
    void removeMember(long apartmentId, long userId, Consumer<Result<Boolean>> callback);

    /** REQ-09 (invited user) */
    void myInvitations(Consumer<Result<List<MyInvitation>>> callback);

    void accept(long invitationId, Consumer<Result<ApartmentSummary>> callback);

    void decline(long invitationId, Consumer<Result<Boolean>> callback);
}
