package com.labelhub.modules.ai.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiReviewRetryConfig {

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService aiReviewRetryExecutor() {
        return Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "ai-retry");
            t.setDaemon(true);
            return t;
        });
    }
}
