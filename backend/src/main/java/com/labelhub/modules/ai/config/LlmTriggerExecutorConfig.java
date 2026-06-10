package com.labelhub.modules.ai.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(LlmTriggerExecutorProperties.class)
public class LlmTriggerExecutorConfig {

    public static final String LLM_TRIGGER_TASK_EXECUTOR = "llmTriggerTaskExecutor";

    @Bean(name = LLM_TRIGGER_TASK_EXECUTOR)
    public ThreadPoolTaskExecutor llmTriggerTaskExecutor(LlmTriggerExecutorProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.corePoolSize());
        executor.setMaxPoolSize(properties.maxPoolSize());
        executor.setQueueCapacity(properties.queueCapacity());
        executor.setKeepAliveSeconds(properties.keepAliveSeconds());
        executor.setThreadNamePrefix("llm-trigger-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(properties.awaitTerminationSeconds());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
