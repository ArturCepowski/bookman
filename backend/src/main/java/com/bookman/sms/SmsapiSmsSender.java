package com.bookman.sms;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * REQ-13 production provider. ASSUMPTION(REQ-13): SMSAPI.pl until the provider is chosen (Q-05); enabled with
 * BOOKMAN_SMS_PROVIDER=smsapi and BOOKMAN_SMSAPI_TOKEN. Another provider is one more {@link SmsSender}.
 */
@Component
@ConditionalOnProperty(name = "bookman.sms.provider", havingValue = "smsapi")
public class SmsapiSmsSender implements SmsSender {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final SmsapiProperties properties;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public SmsapiSmsSender(SmsapiProperties properties) {
        if (properties.token() == null || properties.token().isBlank()) {
            throw new IllegalStateException("BOOKMAN_SMSAPI_TOKEN must be set when BOOKMAN_SMS_PROVIDER=smsapi");
        }
        this.properties = properties;
    }

    @Override
    public void send(String phoneNumber, String text) throws SmsSendException {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("to", phoneNumber.startsWith("+") ? phoneNumber.substring(1) : phoneNumber);
        form.put("message", text);
        form.put("format", "json");
        form.put("encoding", "utf-8");
        if (properties.sender() != null && !properties.sender().isBlank()) {
            form.put("from", properties.sender());
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.url()))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + properties.token())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encode(form)))
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new SmsSendException("SMSAPI unreachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SmsSendException("interrupted", e);
        }
        // SMSAPI reports errors as {"error": <code>, "message": "..."}, sometimes with HTTP 200.
        if (response.statusCode() / 100 != 2 || response.body().contains("\"error\"")) {
            throw new SmsSendException("SMSAPI HTTP " + response.statusCode() + ": " + abbreviate(response.body()));
        }
    }

    private static String encode(Map<String, String> form) {
        return form.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static String abbreviate(String body) {
        return body.length() <= 200 ? body : body.substring(0, 200);
    }
}
