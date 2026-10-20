package com.bookman.app.data.api;

import com.bookman.app.common.Result;
import java.io.IOException;
import retrofit2.Call;
import retrofit2.Response;

public final class ApiCalls {

    private ApiCalls() {
    }

    /** Blocking call; must run off the main thread. */
    public static <T> Result<T> execute(Call<T> call) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                return Result.success(response.body());
            }
            return Result.failure(ApiErrors.from(response));
        } catch (IOException e) {
            return Result.failure(ApiErrors.from(e));
        }
    }

    /** For endpoints answering 204 No Content. */
    public static Result<Boolean> executeNoContent(Call<Void> call) {
        try {
            Response<Void> response = call.execute();
            return response.isSuccessful() ? Result.success(true) : Result.failure(ApiErrors.from(response));
        } catch (IOException e) {
            return Result.failure(ApiErrors.from(e));
        }
    }
}
