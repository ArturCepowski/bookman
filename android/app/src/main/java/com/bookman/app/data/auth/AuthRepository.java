package com.bookman.app.data.auth;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;

public interface AuthRepository {

    boolean isLoggedIn();

    void register(String email, String password, String phoneNumber, String language, Consumer<Result<User>> callback);

    void login(String email, String password, Consumer<Result<User>> callback);

    void loadCurrentUser(Consumer<Result<User>> callback);

    void logout();
}
