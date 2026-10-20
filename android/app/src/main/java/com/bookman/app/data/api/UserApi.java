package com.bookman.app.data.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;

public interface UserApi {

    @GET("api/users/me")
    Call<UserDto> me();

    @PUT("api/users/me")
    Call<UserDto> update(@Body UpdateProfileRequestDto body);
}
