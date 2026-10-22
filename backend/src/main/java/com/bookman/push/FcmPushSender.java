package com.bookman.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Q-24 production channel: Firebase Cloud Messaging HTTP v1. Enabled with BOOKMAN_PUSH_PROVIDER=fcm and
 * BOOKMAN_FCM_CREDENTIALS (see {@link PushConfig}).
 */
public class FcmPushSender implements PushSender {

    /** Must match the channel the Android app creates. */
    static final String ANDROID_CHANNEL = "reminders";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final FcmAccessTokens tokens;
    private final String baseUrl;
    private final ObjectMapper json;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public FcmPushSender(FcmAccessTokens tokens, String baseUrl, ObjectMapper json) {
        this.tokens = tokens;
        this.baseUrl = baseUrl;
        this.json = json;
    }

    @Override
    public void send(String deviceToken, String title, String body) throws PushSendException {
        HttpResponse<String> response;
        try {
            String payload = json.writeValueAsString(Map.of("message", Map.of(
                    "token", deviceToken,
                    "notification", Map.of("title", title, "body", body),
                    "android", Map.of("priority", "HIGH", "notification", Map.of("channel_id", ANDROID_CHANNEL)))));
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create(baseUrl + "/v1/projects/" + tokens.projectId() + "/messages:send"))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + tokens.accessToken())
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new PushSendException("FCM unreachable: " + e.getMessage(), false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PushSendException("interrupted", false);
        }
        int status = response.statusCode();
        if (status / 100 == 2) {
            return;
        }
        String reason = response.body().length() <= 300 ? response.body() : response.body().substring(0, 300);
        // 404 UNREGISTERED / 400 INVALID_ARGUMENT for the token: the app was uninstalled or the token is stale.
        boolean invalidToken = status == 404 || (status == 400 && response.body().contains("registration token"));
        throw new PushSendException("FCM HTTP " + status + ": " + reason, invalidToken);
    }
}
