package com.labelhub.modules.ai.service;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.infrastructure.llm.LlmGateway;
import com.labelhub.infrastructure.llm.LlmGatewayRequest;
import com.labelhub.infrastructure.llm.LlmGatewayResponse;
import com.labelhub.infrastructure.llm.LlmGatewayStatus;
import com.labelhub.infrastructure.llm.LlmMessage;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.domain.SystemActorContext;
import com.labelhub.modules.agent.service.AgentRunService;
import com.labelhub.modules.agent.service.SystemAgentProvider;
import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiReviewRetryService implements AiReviewRetryCallback {

    private static final Logger log = LoggerFactory.getLogger(AiReviewRetryService.class);
    private static final long BASE_DELAY_MS = 2000L;
    private static final int DEFAULT_MAX_RETRY = 3;
    private static final String AGENT_TYPE = "AI_REVIEW";
    private static final String BIZ_TYPE = "AI_REVIEW";

    private final AiReviewResultMapper aiReviewResultMapper;
    private final AiReviewConfigMapper aiReviewConfigMapper;
    private final SubmissionMapper submissionMapper;
    private final TaskMapper taskMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final AiReviewRateLimiter rateLimiter;
    private final LlmGateway llmGateway;
    private final AgentRunService agentRunService;
    private final SystemAgentProvider systemAgentProvider;
    private final AuditAppender auditAppender;
    private final TraceIdProvider traceIdProvider;
    private final AiReviewRetryScheduler retryScheduler;

    public AiReviewRetryService(AiReviewResultMapper aiReviewResultMapper,
                                AiReviewConfigMapper aiReviewConfigMapper,
                                SubmissionMapper submissionMapper,
                                TaskMapper taskMapper,
                                DatasetItemMapper datasetItemMapper,
                                AiReviewRateLimiter rateLimiter,
                                LlmGateway llmGateway,
                                AgentRunService agentRunService,
                                SystemAgentProvider systemAgentProvider,
                                AuditAppender auditAppender,
                                TraceIdProvider traceIdProvider,
                                AiReviewRetryScheduler retryScheduler) {
        this.aiReviewResultMapper = aiReviewResultMapper;
        this.aiReviewConfigMapper = aiReviewConfigMapper;
        this.submissionMapper = submissionMapper;
        this.taskMapper = taskMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.rateLimiter = rateLimiter;
        this.llmGateway = llmGateway;
        this.agentRunService = agentRunService;
        this.systemAgentProvider = systemAgentProvider;
        this.auditAppender = auditAppender;
        this.traceIdProvider = traceIdProvider;
        this.retryScheduler = retryScheduler;
        retryScheduler.setRetryCallback(this);
    }

    public boolean scheduleRetryIfAllowed(AiReviewResult result, AiReviewConfig config,
                                          String failureType) {
        int maxRetry = config.getMaxRetry() != null ? config.getMaxRetry() : DEFAULT_MAX_RETRY;
        if (result.getRetryCount() >= maxRetry) {
            return false;
        }
        int nextRetryCount = result.getRetryCount() + 1;
        Duration delay = calculateBackoff(result.getRetryCount(), failureType);
        LocalDateTime nextRetryAt = LocalDateTime.now().plus(delay);

        aiReviewResultMapper.updateForRetry(
                result.getSubmissionId(),
                result.getRetryCount(),
                AiReviewStatus.PENDING.name(),
                nextRetryCount,
                nextRetryAt,
                result.getEffectiveRunId(),
                result.getErrorCode(),
                result.getErrorMessage(),
                result.getRawResponse()
        );
        retryScheduler.scheduleRetry(result.getSubmissionId(), delay);
        log.info("Scheduled retry #{} for submission {} in {}ms",
                nextRetryCount, result.getSubmissionId(), delay.toMillis());
        return true;
    }

    @Override
    public void onRetry(Long submissionId) {
        AiReviewResult result = aiReviewResultMapper.selectBySubmissionId(submissionId);
        if (result == null || result.getStatus() != AiReviewStatus.PENDING) {
            return;
        }
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return;
        }
        Task task = taskMapper.selectById(submission.getTaskId());
        AiReviewConfig config = loadConfig(task);
        if (config == null) {
            markManualRequired(result, "CONFIG_NOT_FOUND", "AI review config not found");
            return;
        }
        DatasetItem datasetItem = datasetItemMapper.selectById(submission.getDatasetItemId());
        String promptSnapshot = result.getPromptSnapshot();

        AgentRun agentRun = agentRunService.create(AGENT_TYPE, submissionId, config.getProviderId(),
                config.getModelName(), config.getPromptVersion(), promptSnapshot);
        agentRunService.start(agentRun.getId());

        if (!rateLimiter.acquire(submission.getTaskId(), config.getProviderId())) {
            agentRunService.fail(agentRun.getId(), AgentRunStatus.RATE_LIMITED, "Rate limited");
            handleRetryFailure(result, config, agentRun.getId(), "RATE_LIMITED", "Rate limited", null);
            return;
        }

        LlmGatewayResponse response = llmGateway.review(new LlmGatewayRequest(
                config.getProviderId(), config.getModelName(),
                List.of(new LlmMessage("system", "You are LabelHub AI reviewer. Return valid JSON only."),
                        new LlmMessage("user", promptSnapshot))
        ));

        if (response.status() != LlmGatewayStatus.SUCCESS) {
            agentRunService.fail(agentRun.getId(), AgentRunStatus.FAILED, response.errorMessage());
            handleRetryFailure(result, config, agentRun.getId(),
                    response.errorCode(), response.errorMessage(), response.rawResponse());
            return;
        }

        Map<String, Object> structured = response.structuredJson();
        if (structured == null || !structured.containsKey("decision")) {
            agentRunService.fail(agentRun.getId(), AgentRunStatus.MANUAL_REQUIRED, "Missing decision");
            markManualRequired(result, "INVALID_OUTPUT", "AI review decision is required");
            return;
        }

        agentRunService.complete(agentRun.getId(), response.rawResponse());
        BigDecimal avgScore = structured.get("averageScore") instanceof Number n
                ? BigDecimal.valueOf(n.doubleValue()) : null;
        aiReviewResultMapper.updateForSuccess(
                submissionId, AiReviewStatus.SUCCESS.name(), agentRun.getId(),
                String.valueOf(structured.get("decision")), avgScore,
                toJsonSafe(structured.getOrDefault("dimensionScores", Map.of())),
                toJsonSafe(structured.getOrDefault("riskFlags", List.of())),
                structured.get("suggestion") != null ? String.valueOf(structured.get("suggestion")) : null,
                response.rawResponse()
        );
        appendAudit(submissionId, agentRun.getId(), AiReviewStatus.SUCCESS);
        log.info("Retry succeeded for submission {}", submissionId);
    }

    private void handleRetryFailure(AiReviewResult result, AiReviewConfig config,
                                    Long agentRunId, String errorCode, String errorMessage,
                                    String rawResponse) {
        result.setEffectiveRunId(agentRunId);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        result.setRawResponse(rawResponse);
        if (!scheduleRetryIfAllowed(result, config, errorCode)) {
            markManualRequired(result, errorCode, errorMessage);
        }
    }

    private void markManualRequired(AiReviewResult result, String errorCode, String errorMessage) {
        aiReviewResultMapper.updateForRetry(
                result.getSubmissionId(), result.getRetryCount(),
                AiReviewStatus.MANUAL_REQUIRED.name(), result.getRetryCount(),
                null, result.getEffectiveRunId(), errorCode, errorMessage, result.getRawResponse()
        );
        appendAudit(result.getSubmissionId(), result.getEffectiveRunId(), AiReviewStatus.MANUAL_REQUIRED);
        log.info("Max retries reached for submission {}, marked MANUAL_REQUIRED", result.getSubmissionId());
    }

    private Duration calculateBackoff(int retryCount, String failureType) {
        long base = BASE_DELAY_MS * (1L << retryCount);
        if ("RATE_LIMITED".equals(failureType)) {
            base *= 3;
        }
        long jitter = ThreadLocalRandom.current().nextLong(-base / 4, base / 4);
        return Duration.ofMillis(Math.max(base + jitter, BASE_DELAY_MS));
    }

    private AiReviewConfig loadConfig(Task task) {
        if (task == null || task.getAiReviewConfigId() == null) {
            return null;
        }
        return aiReviewConfigMapper.selectById(task.getAiReviewConfigId());
    }

    private void appendAudit(Long submissionId, Long agentRunId, AiReviewStatus status) {
        SystemActorContext actor = systemAgentProvider.get();
        String action = status == AiReviewStatus.SUCCESS ? "AI_REVIEW_RETRY_COMPLETED" : "AI_REVIEW_RETRY_EXHAUSTED";
        auditAppender.append(new AuditCommand(SystemActorContext.ACTOR_TYPE, actor.agentId(),
                BIZ_TYPE, submissionId, action, null, Map.of("status", status.name(), "agentRunId", agentRunId),
                traceIdProvider.currentTraceId(), agentRunId));
    }

    private String toJsonSafe(Object value) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }
}
