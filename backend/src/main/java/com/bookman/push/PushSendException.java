package com.bookman.push;

public class PushSendException extends Exception {

    private final boolean invalidToken;

    public PushSendException(String message, boolean invalidToken) {
        super(message);
        this.invalidToken = invalidToken;
    }

    /** True if retrying makes no sense because the device token is no longer registered. */
    public boolean isInvalidToken() {
        return invalidToken;
    }
}
