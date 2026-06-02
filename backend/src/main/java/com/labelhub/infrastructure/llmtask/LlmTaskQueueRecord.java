package com.labelhub.infrastructure.llmtask;

public record LlmTaskQueueRecord(String messageId, LlmTaskQueueMessage message) {
}
