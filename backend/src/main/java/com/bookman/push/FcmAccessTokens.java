package com.bookman.push;

import java.io.IOException;

/** OAuth2 access tokens for the FCM HTTP v1 API. */
public interface FcmAccessTokens {

    String projectId();

    String accessToken() throws IOException;
}
