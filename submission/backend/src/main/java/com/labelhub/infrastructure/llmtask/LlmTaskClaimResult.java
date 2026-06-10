package com.labelhub.infrastructure.llmtask;

import java.util.List;

public record LlmTaskClaimResult(String nextStartMessageId, List<LlmTaskQueueRecord> records) {
}
