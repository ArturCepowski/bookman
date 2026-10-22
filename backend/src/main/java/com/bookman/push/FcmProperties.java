package com.bookman.push;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param credentials path to the Firebase service-account JSON (BOOKMAN_FCM_CREDENTIALS) – a secret, never committed
 * @param url         FCM HTTP v1 base URL (overridable for tests)
 */
@ConfigurationProperties(prefix = "bookman.push.fcm")
public record FcmProperties(String credentials, String url) {
}
