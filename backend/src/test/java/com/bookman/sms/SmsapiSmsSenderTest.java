package com.bookman.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Against a local stand-in for the SMSAPI endpoint – no real SMS, no external call. */
class SmsapiSmsSenderTest {

    private HttpServer server;
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<String> auth = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String reply = "{\"count\":1,\"list\":[{\"id\":\"1\",\"points\":0.16}]}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/sms.do", ex -> {
            body.set(URLDecoder.decode(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8));
            auth.set(ex.getRequestHeaders().getFirst("Authorization"));
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
    void REQ13_productionProviderSendsTextWithTokenFromConfiguration() throws Exception {
        sender("secret-token", "Bookman").send("+48600100200", "Bookman: sprzątanie – Słoneczne");

        assertThat(auth.get()).isEqualTo("Bearer secret-token");
        assertThat(body.get()).contains("to=48600100200").contains("message=Bookman: sprzątanie – Słoneczne")
                .contains("from=Bookman").contains("encoding=utf-8");
    }

    @Test
    void REQ13_providerErrorBecomesSendFailure() {
        reply = "{\"error\":101,\"message\":\"Authorization failed\"}";
        assertThatThrownBy(() -> sender("bad", "").send("+48600100200", "x")).isInstanceOf(SmsSendException.class);

        status = 500;
        reply = "oops";
        assertThatThrownBy(() -> sender("t", "").send("+48600100200", "x")).isInstanceOf(SmsSendException.class);
    }

    @Test
    void REQ13_missingTokenFailsFast() {
        assertThatThrownBy(() -> sender(" ", "")).isInstanceOf(IllegalStateException.class);
    }

    private SmsapiSmsSender sender(String token, String from) {
        return new SmsapiSmsSender(new SmsapiProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/sms.do", token, from));
    }
}
