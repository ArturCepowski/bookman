package com.bookman.app.data.session;

import androidx.annotation.Nullable;

public interface SessionStore {

    @Nullable
    String token();

    void save(String token);

    void clear();
}
