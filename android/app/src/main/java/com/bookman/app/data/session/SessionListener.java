package com.bookman.app.data.session;

/** Lets other parts of the app react to login/logout without the auth code knowing about them. */
public interface SessionListener {

    void onLoggedIn();

    /** @param previousSessionToken the token that was valid until now (the session is already cleared) */
    void onLoggedOut(String previousSessionToken);
}
