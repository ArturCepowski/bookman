package com.bookman.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param url    API endpoint (BOOKMAN_SMSAPI_URL)
 * @param token  OAuth token from the SMSAPI panel (BOOKMAN_SMSAPI_TOKEN) – a secret, never committed
 * @param sender approved sender name (BOOKMAN_SMSAPI_SENDER); empty = provider default
 */
@ConfigurationProperties(prefix = "bookman.sms.smsapi")
public record SmsapiProperties(String url, String token, String sender) {
}
