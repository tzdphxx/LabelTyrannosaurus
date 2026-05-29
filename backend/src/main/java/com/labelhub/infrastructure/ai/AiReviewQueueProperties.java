package com.labelhub.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;

@ConfigurationProperties(prefix = "labelhub.redis.ai-review-queue")
public record AiReviewQueueProperties(
        String consumerGroup,
        int defaultBatchSize,
        Duration readWait,
        Duration pendingMinIdle
) {

    public AiReviewQueueProperties {
        if (!StringUtils.hasText(consumerGroup)) {
            consumerGroup = "ai-review-workers";
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
