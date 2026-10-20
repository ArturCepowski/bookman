package com.bookman.app.fakes;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;
import com.bookman.app.data.auth.AuthRepository;
import com.bookman.app.data.auth.User;

/** Answers synchronously with a preset result and records the last call. */
public class FakeAuthRepository implements AuthRepository {

    public Result<User> nextResult = Result.success(new User(1, "anna@example.com", "+48600100200", "EN"));
    public boolean loggedIn;
    public int calls;
    public String lastEmail;
    public String lastPassword;
    public String lastPhone;
    public String lastLanguage;

    @Override
    public boolean isLoggedIn() {
        return loggedIn;
    }

    @Override
    public void register(String email, String password, String phoneNumber, String language,
            Consumer<Result<User>> callback) {
        calls++;
        lastEmail = email;
        lastPassword = password;
        lastPhone = phoneNumber;
        lastLanguage = language;
        complete(callback);
    }

    @Override
    public void login(String email, String password, Consumer<Result<User>> callback) {
        calls++;
        lastEmail = email;
        lastPassword = password;
        complete(callback);
    }

    @Override
    public void loadCurrentUser(Consumer<Result<User>> callback) {
        calls++;
        callback.accept(nextResult);
    }

    @Override
    public void logout() {
        loggedIn = false;
    }

    private void complete(Consumer<Result<User>> callback) {
        loggedIn = nextResult.isSuccess();
        callback.accept(nextResult);
    }
}
