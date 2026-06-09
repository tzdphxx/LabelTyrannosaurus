package com.labelhub.modules.ai.service;

import com.labelhub.infrastructure.llmtask.LlmTaskHandler;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueMessage;
import com.labelhub.infrastructure.llmtask.LlmTaskStatus;
import com.labelhub.infrastructure.llmtask.LlmTaskType;
import com.labelhub.modules.ai.domain.LlmTriggerRun;
import com.labelhub.modules.ai.mapper.LlmTriggerRunMapper;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LlmTriggerTaskHandler implements LlmTaskHandler {

    private static final Set<String> FINAL_STATUSES = Set.of(
            LlmTaskStatus.SUCCESS.name(),
            LlmTaskStatus.FAILED.name(),
            LlmTaskStatus.RATE_LIMITED.name(),
            LlmTaskStatus.MANUAL_REQUIRED.name());

    private final LlmTriggerService llmTriggerService;
    private final LlmTriggerRunMapper llmTriggerRunMapper;

    public LlmTriggerTaskHandler(LlmTriggerService llmTriggerService,
                                 LlmTriggerRunMapper llmTriggerRunMapper) {
        this.llmTriggerService = llmTriggerService;
        this.llmTriggerRunMapper = llmTriggerRunMapper;
    }

    @Override
    public LlmTaskType taskType() {
        return LlmTaskType.LLM_TRIGGER;
    }

    @Override
    public boolean isCompleted(LlmTaskQueueMessage message) {
        LlmTriggerRun run = llmTriggerRunMapper.selectById(message.triggerRunId());
        return run == null || FINAL_STATUSES.contains(run.getStatus());
    }

    @Override
    public void handle(LlmTaskQueueMessage message) {
        llmTriggerService.executeQueuedTrigger(message.triggerRunId());
    }

    @Override
    public void onFailure(LlmTaskQueueMessage message, Exception exception) {
        llmTriggerService.failQueuedTrigger(message.triggerRunId(),
                "LLM_TASK_EXCEPTION", exception.getMessage());
    }
}
