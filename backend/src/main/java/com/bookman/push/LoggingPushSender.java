package com.bookman.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Development implementation: logs instead of sending. Default unless BOOKMAN_PUSH_PROVIDER=fcm. */
@Component
@ConditionalOnProperty(name = "bookman.push.provider", havingValue = "log", matchIfMissing = true)
public class LoggingPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushSender.class);

    @Override
    public void send(String deviceToken, String title, String body) {
        String device = deviceToken.length() <= 8 ? deviceToken : "…" + deviceToken.substring(deviceToken.length() - 8);
        log.info("Push (not sent, development mode) to device {}: {} – {}", device, title, body);
    }
}
