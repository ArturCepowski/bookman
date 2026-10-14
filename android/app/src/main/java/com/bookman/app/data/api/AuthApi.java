package com.bookman.app.data.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {

    @POST("api/auth/register")
    Call<AuthResponseDto> register(@Body RegisterRequestDto body);

    @POST("api/auth/login")
    Call<AuthResponseDto> login(@Body LoginRequestDto body);
}
