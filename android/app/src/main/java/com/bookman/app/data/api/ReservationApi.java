package com.bookman.app.data.api;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ReservationApi {

    @GET("api/apartments/{apartmentId}/reservations")
    Call<List<ReservationDto>> list(@Path("apartmentId") long apartmentId);

    @POST("api/apartments/{apartmentId}/reservations/import")
    Call<ImportResultDto> importNow(@Path("apartmentId") long apartmentId);

    @PUT("api/apartments/{apartmentId}/reservations/{reservationId}/cleaning-status")
    Call<ReservationDto> changeCleaningStatus(@Path("apartmentId") long apartmentId,
            @Path("reservationId") long reservationId, @Body CleaningStatusRequestDto body);
}
