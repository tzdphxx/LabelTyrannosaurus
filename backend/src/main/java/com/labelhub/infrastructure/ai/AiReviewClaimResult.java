package com.labelhub.infrastructure.ai;

import java.util.List;

public record AiReviewClaimResult(String nextStartMessageId, List<AiReviewQueueRecord> records) {
}
