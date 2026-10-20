package com.bookman.app.data.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface DeviceApi {

    @POST("api/users/me/devices")
    Call<Void> register(@Body DeviceRequestDto body);

    /** Called during logout: the session is already cleared, so the old token is passed explicitly. */
    @POST("api/users/me/devices/unregister")
    Call<Void> unregister(@Header("Authorization") String authorization, @Body DeviceRequestDto body);
}
