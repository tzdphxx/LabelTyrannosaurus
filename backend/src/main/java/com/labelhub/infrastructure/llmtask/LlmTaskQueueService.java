package com.labelhub.infrastructure.llmtask;

import java.time.Duration;
import java.util.List;

public interface LlmTaskQueueService {

    String enqueue(LlmTaskQueueMessage message);

    List<LlmTaskQueueRecord> read(LlmTaskType taskType, String consumerName, int count, Duration waitTime);

    LlmTaskClaimResult claimStale(
            LlmTaskType taskType,
            String consumerName,
            Duration minIdleTime,
            String startMessageId,
            int count
    );

    boolean ack(LlmTaskType taskType, String messageId);
}
