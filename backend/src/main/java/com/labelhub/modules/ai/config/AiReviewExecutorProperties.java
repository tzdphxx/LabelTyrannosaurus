package com.labelhub.modules.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "labelhub.ai.review.executor")
public record AiReviewExecutorProperties(
        int corePoolSize,
        int maxPoolSize,
        int queueCapacity,
        int keepAliveSeconds,
        int awaitTerminationSeconds
) {

    public AiReviewExecutorProperties {
        if (corePoolSize <= 0) {
            corePoolSize = 4;
        }
        if (maxPoolSize < corePoolSize) {
            maxPoolSize = Math.max(corePoolSize, 8);
        }
        if (queueCapacity <= 0) {
            queueCapacity = 200;
        }
        if (keepAliveSeconds <= 0) {
            keepAliveSeconds = 60;
        }
        if (awaitTerminationSeconds <= 0) {
            awaitTerminationSeconds = 60;
        }
    }
}
