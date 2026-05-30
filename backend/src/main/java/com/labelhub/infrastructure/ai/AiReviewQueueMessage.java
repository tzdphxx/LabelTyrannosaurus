package com.labelhub.infrastructure.ai;

import java.time.Instant;

public record AiReviewQueueMessage(
        Long taskId,
        Long submissionId,
        Long assignmentId,
        Long labelerId,
        String traceId,
        Integer retryCount,
        Instant createdAt
) {
}
