package com.bookman.sms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class SmsSenderConfigTest {

    @Configuration
    @EnableConfigurationProperties(SmsapiProperties.class)
    @Import({LoggingSmsSender.class, SmsapiSmsSender.class})
    static class Senders {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Senders.class)
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class));

    @Test
    void REQ13_developmentDefaultOnlyLogs() {
        runner.run(ctx -> assertThat(ctx).getBean(SmsSender.class).isInstanceOf(LoggingSmsSender.class));
    }

    @Test
    void REQ13_productionProviderIsSelectedByEnvironment() {
        runner.withPropertyValues("bookman.sms.provider=smsapi", "bookman.sms.smsapi.url=https://api.smsapi.pl/sms.do",
                        "bookman.sms.smsapi.token=t")
                .run(ctx -> assertThat(ctx).getBean(SmsSender.class).isInstanceOf(SmsapiSmsSender.class));
    }
}
