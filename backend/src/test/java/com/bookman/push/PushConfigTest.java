package com.bookman.push;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class PushConfigTest {

    @Configuration
    @EnableConfigurationProperties(FcmProperties.class)
    @Import({LoggingPushSender.class, PushConfig.class})
    static class Senders {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Senders.class)
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class));

    @Test
    void REQ13_developmentDefaultOnlyLogs() {
        runner.run(ctx -> assertThat(ctx).getBean(PushSender.class).isInstanceOf(LoggingPushSender.class));
    }

    @Test
    void REQ13_fcmWithoutCredentialsFailsFastAtStartup() {
        runner.withPropertyValues("bookman.push.provider=fcm", "bookman.push.fcm.url=https://fcm.googleapis.com")
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("BOOKMAN_FCM_CREDENTIALS"));
    }
}
