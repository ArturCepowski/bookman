package com.bookman.reservation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableScheduling
public class ImportConfig {

    static final String EXECUTOR = "reservationImportExecutor";

    /** Q-13: apartments are imported in parallel on a small fixed pool. */
    @Bean(name = EXECUTOR)
    ThreadPoolTaskExecutor reservationImportExecutor() {
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setQueueCapacity(10_000);
        executor.setThreadNamePrefix("import-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }
}
