package com.bookman.app.data.auth;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;
import com.bookman.app.data.api.ApiCalls;
import com.bookman.app.data.api.AuthApi;
import com.bookman.app.data.api.AuthResponseDto;
import com.bookman.app.data.api.LoginRequestDto;
import com.bookman.app.data.api.RegisterRequestDto;
import com.bookman.app.data.api.UserDto;
import com.bookman.app.data.session.SessionListener;
import com.bookman.app.data.session.SessionStore;
import java.util.concurrent.Executor;
import retrofit2.Call;

/** Runs blocking Retrofit calls on {@code io}; callbacks are invoked on that executor. */
public class AuthRepositoryImpl implements AuthRepository {

    private final AuthApi api;
    private final SessionStore sessionStore;
    private final Executor io;
    private final SessionListener listener;

    public AuthRepositoryImpl(AuthApi api, SessionStore sessionStore, Executor io, SessionListener listener) {
        this.api = api;
        this.sessionStore = sessionStore;
        this.io = io;
        this.listener = listener;
    }

    @Override
    public boolean isLoggedIn() {
        return sessionStore.token() != null;
    }

    @Override
    public void register(String email, String password, String phoneNumber, String language,
            Consumer<Result<User>> callback) {
        authenticate(api.register(new RegisterRequestDto(email, password, phoneNumber, language)), callback);
    }

    @Override
    public void login(String email, String password, Consumer<Result<User>> callback) {
        authenticate(api.login(new LoginRequestDto(email, password)), callback);
    }

    @Override
    public void logout() {
        // REQ-01 / Q-15 (confirmed): logout is client-side (JWT is stateless); the token expires on the server.
        String previous = sessionStore.token();
        sessionStore.clear();
        listener.onLoggedOut(previous);
    }

    private void authenticate(Call<AuthResponseDto> call, Consumer<Result<User>> callback) {
        io.execute(() -> {
            Result<AuthResponseDto> result = ApiCalls.execute(call);
            if (!result.isSuccess()) {
                callback.accept(Result.failure(result.error()));
                return;
            }
            sessionStore.save(result.value().token);
            listener.onLoggedIn();
            callback.accept(Result.success(toUser(result.value().user)));
        });
    }

    static User toUser(UserDto dto) {
        return new User(dto.id, dto.email, dto.phoneNumber, dto.language);
    }
}
