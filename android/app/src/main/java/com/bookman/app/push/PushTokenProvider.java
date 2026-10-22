package com.bookman.app.push;

import androidx.core.util.Consumer;

/** Source of this phone's push token; hides Firebase so the logic can be unit-tested. */
public interface PushTokenProvider {

    /** Delivers the token, or null when push is not available (e.g. no Firebase config in this build). */
    void currentToken(Consumer<String> callback);
}
