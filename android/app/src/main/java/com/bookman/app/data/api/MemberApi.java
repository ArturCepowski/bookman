package com.bookman.app.data.api;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface MemberApi {

    @GET("api/apartments/{apartmentId}/members")
    Call<MembersOverviewDto> overview(@Path("apartmentId") long apartmentId);

    @POST("api/apartments/{apartmentId}/invitations")
    Call<PendingInvitationDto> invite(@Path("apartmentId") long apartmentId, @Body InviteRequestDto body);

    @DELETE("api/apartments/{apartmentId}/invitations/{invitationId}")
    Call<Void> cancelInvitation(@Path("apartmentId") long apartmentId, @Path("invitationId") long invitationId);

    @DELETE("api/apartments/{apartmentId}/members/{userId}")
    Call<Void> removeMember(@Path("apartmentId") long apartmentId, @Path("userId") long userId);

    @GET("api/invitations")
    Call<List<MyInvitationDto>> myInvitations();

    @POST("api/invitations/{invitationId}/accept")
    Call<ApartmentDto> accept(@Path("invitationId") long invitationId);

    @POST("api/invitations/{invitationId}/decline")
    Call<Void> decline(@Path("invitationId") long invitationId);
}
