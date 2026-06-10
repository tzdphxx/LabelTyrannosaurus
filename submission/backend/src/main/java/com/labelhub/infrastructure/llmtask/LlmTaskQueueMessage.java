package com.labelhub.infrastructure.llmtask;

import java.time.Instant;

public record LlmTaskQueueMessage(
        LlmTaskType taskType,
        Long bizId,
        Long taskId,
        Long assignmentId,
        Long submissionId,
        Long preAnnotationId,
        Long triggerRunId,
        Long agentRunId,
        String traceId,
        Integer retryCount,
        Instant createdAt
) {
}
