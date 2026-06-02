package com.labelhub.infrastructure.llmtask;

public interface LlmTaskHandler {

    LlmTaskType taskType();

    boolean isCompleted(LlmTaskQueueMessage message);

    void handle(LlmTaskQueueMessage message);
}
