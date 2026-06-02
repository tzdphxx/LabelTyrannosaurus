package com.labelhub.infrastructure.llmtask;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "labelhub.redis.llm-task-queue")
public record LlmTaskQueueProperties(
        String consumerGroup,
        int defaultBatchSize,
        Duration readWait,
        Duration pendingMinIdle
) {

    public LlmTaskQueueProperties {
        if (!StringUtils.hasText(consumerGroup)) {
            consumerGroup = "labelhub-llm-workers";
        }
        if (defaultBatchSize <= 0) {
            defaultBatchSize = 10;
        }
        if (readWait == null || readWait.isNegative() || readWait.isZero()) {
            readWait = Duration.ofSeconds(2);
        }
        if (pendingMinIdle == null || pendingMinIdle.isNegative() || pendingMinIdle.isZero()) {
            pendingMinIdle = Duration.ofMinutes(5);
        }
    }
}
