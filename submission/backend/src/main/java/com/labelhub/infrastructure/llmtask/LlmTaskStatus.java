package com.labelhub.infrastructure.llmtask;

public enum LlmTaskStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    RATE_LIMITED,
    MANUAL_REQUIRED
}
