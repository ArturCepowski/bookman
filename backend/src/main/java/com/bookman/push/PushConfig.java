package com.bookman.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PushConfig {

    @Bean
    @ConditionalOnProperty(name = "bookman.push.provider", havingValue = "fcm")
    PushSender fcmPushSender(FcmProperties properties, ObjectMapper json) {
        return new FcmPushSender(new ServiceAccountFcmAccessTokens(properties.credentials()), properties.url(), json);
    }
}
