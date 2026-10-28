package com.bookman.app.fakes;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;
import com.bookman.app.data.auth.User;
import com.bookman.app.data.profile.ProfileRepository;

public class FakeProfileRepository implements ProfileRepository {

    public Result<User> loadResult = Result.success(new User(1, "anna@example.com", "+48600100200", "EN"));
    /** When set, update() fails or answers with this instead of echoing the request. */
    public Result<User> updateResult;
    public String lastPhone;
    public String lastLanguage;
    public int updates;

    @Override
    public void load(Consumer<Result<User>> callback) {
        callback.accept(loadResult);
    }

    @Override
    public void update(String phoneNumber, String language, Consumer<Result<User>> callback) {
        updates++;
        lastPhone = phoneNumber;
        lastLanguage = language;
        callback.accept(updateResult != null ? updateResult
                : Result.success(new User(1, "anna@example.com", phoneNumber, language)));
    }
}
