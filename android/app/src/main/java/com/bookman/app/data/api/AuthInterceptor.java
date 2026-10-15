package com.bookman.app.data.api;

import com.bookman.app.data.session.SessionStore;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Adds the session token and drops the session when the backend says it is no longer valid. */
public class AuthInterceptor implements Interceptor {

    private final SessionStore sessionStore;

    public AuthInterceptor(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String token = sessionStore.token();
        Request request = chain.request();
        if (token == null) {
            return chain.proceed(request);
        }
        Response response = chain.proceed(request.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build());
        if (response.code() == 401) {
            sessionStore.clear();
        }
        return response;
    }
}
