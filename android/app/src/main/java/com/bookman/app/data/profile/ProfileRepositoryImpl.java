package com.bookman.app.data.profile;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;
import com.bookman.app.data.api.ApiCalls;
import com.bookman.app.data.api.UpdateProfileRequestDto;
import com.bookman.app.data.api.UserApi;
import com.bookman.app.data.api.UserDto;
import com.bookman.app.data.auth.User;
import java.util.concurrent.Executor;
import retrofit2.Call;

public class ProfileRepositoryImpl implements ProfileRepository {

    private final UserApi api;
    private final Executor io;

    public ProfileRepositoryImpl(UserApi api, Executor io) {
        this.api = api;
        this.io = io;
    }

    @Override
    public void load(Consumer<Result<User>> callback) {
        run(api.me(), callback);
    }

    @Override
    public void update(String phoneNumber, String language, Consumer<Result<User>> callback) {
        run(api.update(new UpdateProfileRequestDto(phoneNumber, language)), callback);
    }

    private void run(Call<UserDto> call, Consumer<Result<User>> callback) {
        io.execute(() -> {
            Result<UserDto> result = ApiCalls.execute(call);
            callback.accept(result.isSuccess()
                    ? Result.success(new User(result.value().id, result.value().email,
                            result.value().phoneNumber, result.value().language))
                    : Result.failure(result.error()));
        });
    }
}
