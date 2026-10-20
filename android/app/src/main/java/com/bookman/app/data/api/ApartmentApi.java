package com.bookman.app.data.api;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApartmentApi {

    @GET("api/apartments")
    Call<List<ApartmentDto>> list();

    @GET("api/apartments/{id}")
    Call<ApartmentDto> get(@Path("id") long id);

    @POST("api/apartments")
    Call<ApartmentDto> create(@Body ApartmentRequestDto body);

    @PUT("api/apartments/{id}")
    Call<ApartmentDto> update(@Path("id") long id, @Body ApartmentRequestDto body);

    @DELETE("api/apartments/{id}")
    Call<Void> delete(@Path("id") long id);

    @GET("api/apartments/{id}/notification-settings")
    Call<NotificationSettingsDto> notificationSettings(@Path("id") long id);

    @PUT("api/apartments/{id}/notification-settings")
    Call<NotificationSettingsDto> updateNotificationSettings(@Path("id") long id, @Body NotificationSettingsDto body);
}
