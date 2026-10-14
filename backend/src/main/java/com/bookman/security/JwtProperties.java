package com.bookman.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secret HMAC key, at least 32 bytes; supplied via BOOKMAN_JWT_SECRET, never committed
 * @param ttl    how long a login session lasts
 */
@ConfigurationProperties(prefix = "bookman.jwt")
public record JwtProperties(String secret, Duration ttl) {
}
