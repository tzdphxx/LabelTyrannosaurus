package com.labelhub.modules.ai.service;

import com.labelhub.infrastructure.llmtask.LlmTaskHandler;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueMessage;
import com.labelhub.infrastructure.llmtask.LlmTaskType;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AiReviewLlmTaskHandler implements LlmTaskHandler {

    private static final Set<AiReviewStatus> FINAL_STATUSES = Set.of(
            AiReviewStatus.SUCCESS,
            AiReviewStatus.MANUAL_REQUIRED);

    private final AiAutoReviewService aiAutoReviewService;
    private final AiReviewResultMapper aiReviewResultMapper;

    public AiReviewLlmTaskHandler(AiAutoReviewService aiAutoReviewService,
                                  AiReviewResultMapper aiReviewResultMapper) {
        this.aiAutoReviewService = aiAutoReviewService;
        this.aiReviewResultMapper = aiReviewResultMapper;
    }

    @Override
    public LlmTaskType taskType() {
        return LlmTaskType.AI_REVIEW;
    }

    @Override
    public boolean isCompleted(LlmTaskQueueMessage message) {
        AiReviewResult result = aiReviewResultMapper.selectBySubmissionId(message.submissionId());
        if (result == null) {
            return false;
        }
        if (FINAL_STATUSES.contains(result.getStatus())) {
            return true;
        }
        return (result.getStatus() == AiReviewStatus.FAILED || result.getStatus() == AiReviewStatus.RATE_LIMITED)
                && result.getNextRetryAt() == null;
    }

    @Override
    public void handle(LlmTaskQueueMessage message) {
        AiReviewResult result = aiReviewResultMapper.selectBySubmissionId(message.submissionId());
        if (result == null) {
            aiAutoReviewService.executeQueuedReview(message.submissionId(), message.agentRunId());
            return;
        }
        aiAutoReviewService.retryReview(message.submissionId());
    }

    @Override
    public void onFailure(LlmTaskQueueMessage message, Exception exception) {
        aiAutoReviewService.failQueuedReview(message.submissionId(), message.agentRunId(),
                "LLM_TASK_EXCEPTION", exception.getMessage());
    }
}
