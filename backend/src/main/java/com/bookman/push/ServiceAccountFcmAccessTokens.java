package com.bookman.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/** Reads the service-account key once; the library refreshes the short-lived access token when needed. */
class ServiceAccountFcmAccessTokens implements FcmAccessTokens {

    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final GoogleCredentials credentials;
    private final String projectId;

    ServiceAccountFcmAccessTokens(String credentialsPath) {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            throw new IllegalStateException("BOOKMAN_FCM_CREDENTIALS must point to the Firebase service-account JSON "
                    + "when BOOKMAN_PUSH_PROVIDER=fcm");
        }
        try (InputStream in = new FileInputStream(credentialsPath)) {
            ServiceAccountCredentials account = ServiceAccountCredentials.fromStream(in);
            this.projectId = account.getProjectId();
            this.credentials = account.createScoped(List.of(SCOPE));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read Firebase service account " + credentialsPath, e);
        }
    }

    @Override
    public String projectId() {
        return projectId;
    }

    @Override
    public synchronized String accessToken() throws IOException {
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }
}
