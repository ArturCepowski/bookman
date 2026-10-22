package com.bookman.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Against a local stand-in for the FCM HTTP v1 endpoint – no Google call, no real notification. */
class FcmPushSenderTest {

    private final ObjectMapper json = new ObjectMapper();
    private HttpServer server;
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> auth = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String reply = "{\"name\":\"projects/bookman-test/messages/1\"}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            path.set(ex.getRequestURI().getPath());
            auth.set(ex.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = reply.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, out.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(out);
            }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void REQ13_sendsNotificationToTheDeviceWithOAuthToken() throws Exception {
        sender().send("device-token", "Bookman – przypomnienie", "Sprzątanie – Słoneczne");

        assertThat(path.get()).isEqualTo("/v1/projects/bookman-test/messages:send");
        assertThat(auth.get()).isEqualTo("Bearer access-123");
        JsonNode message = json.readTree(body.get()).get("message");
        assertThat(message.get("token").asText()).isEqualTo("device-token");
        assertThat(message.get("notification").get("title").asText()).isEqualTo("Bookman – przypomnienie");
        assertThat(message.get("notification").get("body").asText()).isEqualTo("Sprzątanie – Słoneczne");
        assertThat(message.get("android").get("notification").get("channel_id").asText()).isEqualTo("reminders");
    }

    @Test
    void REQ13_unregisteredTokenIsReportedAsInvalid() {
        status = 404;
        reply = "{\"error\":{\"status\":\"NOT_FOUND\",\"details\":[{\"errorCode\":\"UNREGISTERED\"}]}}";

        PushSendException e = catchThrowableOfType(PushSendException.class, () -> sender().send("old", "t", "b"));

        assertThat(e.isInvalidToken()).isTrue();
    }

    @Test
    void REQ13_serverErrorIsRetryable() {
        status = 503;
        reply = "{\"error\":{\"status\":\"UNAVAILABLE\"}}";

        PushSendException e = catchThrowableOfType(PushSendException.class, () -> sender().send("t", "t", "b"));

        assertThat(e.isInvalidToken()).isFalse();
    }

    private FcmPushSender sender() {
        FcmAccessTokens tokens = new FcmAccessTokens() {
            @Override
            public String projectId() {
                return "bookman-test";
            }

            @Override
            public String accessToken() {
                return "access-123";
            }
        };
        return new FcmPushSender(tokens, "http://127.0.0.1:" + server.getAddress().getPort(), json);
    }
}
