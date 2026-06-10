package com.labelhub.infrastructure.ai;

import java.time.Duration;
import java.util.List;

public interface AiReviewQueueService {

    String enqueue(AiReviewQueueMessage message);

    List<AiReviewQueueRecord> read(Long taskId, String consumerName, int count, Duration waitTime);

    AiReviewClaimResult claimStale(
            Long taskId,
            String consumerName,
            Duration minIdleTime,
            String startMessageId,
            int count
    );

    boolean ack(Long taskId, String messageId);
}
