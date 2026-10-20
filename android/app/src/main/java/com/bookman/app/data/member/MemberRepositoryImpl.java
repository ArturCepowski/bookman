package com.bookman.app.data.member;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;
import com.bookman.app.data.api.ApartmentDto;
import com.bookman.app.data.api.ApiCalls;
import com.bookman.app.data.api.InviteRequestDto;
import com.bookman.app.data.api.MemberApi;
import com.bookman.app.data.api.MembersOverviewDto;
import com.bookman.app.data.api.MyInvitationDto;
import com.bookman.app.data.api.PendingInvitationDto;
import com.bookman.app.data.apartment.ApartmentSummary;
import com.bookman.app.data.apartment.Role;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class MemberRepositoryImpl implements MemberRepository {

    private final MemberApi api;
    private final Executor io;

    public MemberRepositoryImpl(MemberApi api, Executor io) {
        this.api = api;
        this.io = io;
    }

    @Override
    public void overview(long apartmentId, Consumer<Result<MembersOverview>> callback) {
        io.execute(() -> {
            Result<MembersOverviewDto> result = ApiCalls.execute(api.overview(apartmentId));
            if (!result.isSuccess()) {
                callback.accept(Result.failure(result.error()));
                return;
            }
            List<Member> members = new ArrayList<>();
            for (MembersOverviewDto.MemberDto dto : result.value().members) {
                members.add(new Member(dto.userId, dto.email, Role.valueOf(dto.role)));
            }
            List<PendingInvitation> invitations = new ArrayList<>();
            for (PendingInvitationDto dto : result.value().invitations) {
                invitations.add(toPending(dto));
            }
            callback.accept(Result.success(new MembersOverview(members, invitations)));
        });
    }

    @Override
    public void invite(long apartmentId, String email, Consumer<Result<PendingInvitation>> callback) {
        io.execute(() -> {
            Result<PendingInvitationDto> result = ApiCalls.execute(api.invite(apartmentId, new InviteRequestDto(email)));
            callback.accept(result.isSuccess()
                    ? Result.success(toPending(result.value()))
                    : Result.failure(result.error()));
        });
    }

    @Override
    public void cancelInvitation(long apartmentId, long invitationId, Consumer<Result<Boolean>> callback) {
        io.execute(() -> callback.accept(ApiCalls.executeNoContent(api.cancelInvitation(apartmentId, invitationId))));
    }

    @Override
    public void removeMember(long apartmentId, long userId, Consumer<Result<Boolean>> callback) {
        io.execute(() -> callback.accept(ApiCalls.executeNoContent(api.removeMember(apartmentId, userId))));
    }

    @Override
    public void myInvitations(Consumer<Result<List<MyInvitation>>> callback) {
        io.execute(() -> {
            Result<List<MyInvitationDto>> result = ApiCalls.execute(api.myInvitations());
            if (!result.isSuccess()) {
                callback.accept(Result.failure(result.error()));
                return;
            }
            List<MyInvitation> items = new ArrayList<>();
            for (MyInvitationDto dto : result.value()) {
                items.add(new MyInvitation(dto.id, dto.apartmentId, dto.apartmentName, dto.invitedBy,
                        Instant.parse(dto.expiresAt)));
            }
            callback.accept(Result.success(items));
        });
    }

    @Override
    public void accept(long invitationId, Consumer<Result<ApartmentSummary>> callback) {
        io.execute(() -> {
            Result<ApartmentDto> result = ApiCalls.execute(api.accept(invitationId));
            callback.accept(result.isSuccess()
                    ? Result.success(new ApartmentSummary(result.value().id, result.value().name,
                            Role.valueOf(result.value().role)))
                    : Result.failure(result.error()));
        });
    }

    @Override
    public void decline(long invitationId, Consumer<Result<Boolean>> callback) {
        io.execute(() -> callback.accept(ApiCalls.executeNoContent(api.decline(invitationId))));
    }

    private static PendingInvitation toPending(PendingInvitationDto dto) {
        return new PendingInvitation(dto.id, dto.email, Instant.parse(dto.expiresAt));
    }
}
