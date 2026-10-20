package com.bookman.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** REQ-13 development implementation: logs instead of sending. Default unless BOOKMAN_SMS_PROVIDER is set. */
@Component
@ConditionalOnProperty(name = "bookman.sms.provider", havingValue = "log", matchIfMissing = true)
public class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void send(String phoneNumber, String text) {
        log.info("SMS (not sent, development mode) to {}: {}", phoneNumber, text);
    }
}
