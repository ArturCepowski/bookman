package com.bookman.app.data.profile;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;
import com.bookman.app.data.auth.User;

public interface ProfileRepository {

    void load(Consumer<Result<User>> callback);

    void update(String phoneNumber, String language, Consumer<Result<User>> callback);
}
