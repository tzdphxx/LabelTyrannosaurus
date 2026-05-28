package com.labelhub.modules.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
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
import com.labelhub.modules.ai.dto.AiReviewResultResponse;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAutoReviewService {

    private static final int SUBMISSION_NOT_FOUND = 404701;
    private static final int AI_REVIEW_CONFIG_NOT_FOUND = 404702;
    private static final int AI_REVIEW_INVALID = 400701;
    private static final String BIZ_TYPE = "AI_REVIEW";
    private static final String AGENT_TYPE = "AI_REVIEW";
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final SubmissionMapper submissionMapper;
    private final TaskMapper taskMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final AiReviewConfigMapper aiReviewConfigMapper;
    private final AiReviewResultMapper aiReviewResultMapper;
    private final AiReviewRateLimiter rateLimiter;
    private final LlmGateway llmGateway;
    private final AgentRunService agentRunService;
    private final SystemAgentProvider systemAgentProvider;
    private final AuditAppender auditAppender;
    private final TraceIdProvider traceIdProvider;
    private final ObjectMapper objectMapper;

    public AiAutoReviewService(SubmissionMapper submissionMapper,
                               TaskMapper taskMapper,
                               DatasetItemMapper datasetItemMapper,
                               AiReviewConfigMapper aiReviewConfigMapper,
                               AiReviewResultMapper aiReviewResultMapper,
                               AiReviewRateLimiter rateLimiter,
                               LlmGateway llmGateway,
                               AgentRunService agentRunService,
                               SystemAgentProvider systemAgentProvider,
                               AuditAppender auditAppender,
                               TraceIdProvider traceIdProvider) {
        this(submissionMapper, taskMapper, datasetItemMapper, aiReviewConfigMapper, aiReviewResultMapper, rateLimiter,
                llmGateway, agentRunService, systemAgentProvider, auditAppender, traceIdProvider, new ObjectMapper());
    }

    AiAutoReviewService(SubmissionMapper submissionMapper,
                        TaskMapper taskMapper,
                        DatasetItemMapper datasetItemMapper,
                        AiReviewConfigMapper aiReviewConfigMapper,
                        AiReviewResultMapper aiReviewResultMapper,
                        AiReviewRateLimiter rateLimiter,
                        LlmGateway llmGateway,
                        AgentRunService agentRunService,
                        SystemAgentProvider systemAgentProvider,
                        AuditAppender auditAppender,
                        TraceIdProvider traceIdProvider,
                        ObjectMapper objectMapper) {
        this.submissionMapper = submissionMapper;
        this.taskMapper = taskMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.aiReviewConfigMapper = aiReviewConfigMapper;
        this.aiReviewResultMapper = aiReviewResultMapper;
        this.rateLimiter = rateLimiter;
        this.llmGateway = llmGateway;
        this.agentRunService = agentRunService;
        this.systemAgentProvider = systemAgentProvider;
        this.auditAppender = auditAppender;
        this.traceIdProvider = traceIdProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiReviewResultResponse reviewSubmission(Long submissionId) {
        AiReviewResult existing = aiReviewResultMapper.selectBySubmissionId(submissionId);
        if (existing != null) {
            return toResponse(existing);
        }

        Submission submission = loadSubmission(submissionId);
        Task task = taskMapper.selectById(submission.getTaskId());
        AiReviewConfig config = loadConfig(task);
        DatasetItem datasetItem = datasetItemMapper.selectById(submission.getDatasetItemId());
        String promptSnapshot = buildPromptSnapshot(submission, datasetItem, config);

        AgentRun agentRun = agentRunService.create(AGENT_TYPE, submissionId, config.getProviderId(),
                config.getModelName(), config.getPromptVersion(), promptSnapshot);
        agentRunService.start(agentRun.getId());

        AiReviewResult result;
        if (!rateLimiter.acquire(submission.getTaskId(), config.getProviderId())) {
            agentRunService.fail(agentRun.getId(), AgentRunStatus.RATE_LIMITED, "AI review rate limited");
            result = manualRequired(submission, config, agentRun.getId(), promptSnapshot, null,
                    "RATE_LIMITED", "AI review rate limited");
        } else {
            result = runGateway(submission, config, agentRun, promptSnapshot);
        }

        aiReviewResultMapper.insert(result);
        moveSubmissionToPendingFinal(submission);
        appendAudit(result);
        return toResponse(result);
    }

    private AiReviewResult runGateway(Submission submission, AiReviewConfig config,
                                      AgentRun agentRun, String promptSnapshot) {
        LlmGatewayResponse response = llmGateway.review(new LlmGatewayRequest(
                config.getProviderId(),
                config.getModelName(),
                List.of(
                        new LlmMessage("system", "You are LabelHub AI reviewer. Return valid JSON only."),
                        new LlmMessage("user", promptSnapshot)
                )
        ));
        if (response.status() != LlmGatewayStatus.SUCCESS) {
            agentRunService.fail(agentRun.getId(), AgentRunStatus.FAILED, response.errorMessage());
            return manualRequired(submission, config, agentRun.getId(), promptSnapshot, response.rawResponse(),
                    response.errorCode(), response.errorMessage());
        }

        try {
            AiReviewResult result = successResult(submission, config, agentRun.getId(), promptSnapshot, response);
            agentRunService.complete(agentRun.getId(), toJson(gatewayResponseSnapshot(response)));
            return result;
        } catch (BusinessException ex) {
            agentRunService.fail(agentRun.getId(), AgentRunStatus.MANUAL_REQUIRED, ex.getMessage());
            return manualRequired(submission, config, agentRun.getId(), promptSnapshot, response.rawResponse(),
                    "INVALID_AI_REVIEW_OUTPUT", ex.getMessage());
        }
    }

    private AiReviewResult successResult(Submission submission, AiReviewConfig config, Long agentRunId,
                                         String promptSnapshot, LlmGatewayResponse response) {
        Map<String, Object> structuredJson = response.structuredJson();
        if (structuredJson == null || !structuredJson.containsKey("decision")) {
            throw new BusinessException(AI_REVIEW_INVALID, "AI review decision is required");
        }
        AiReviewResult result = baseResult(submission, config, agentRunId, promptSnapshot);
        result.setStatus(AiReviewStatus.SUCCESS);
        result.setDecision(String.valueOf(structuredJson.get("decision")));
        result.setAverageScore(asBigDecimal(structuredJson.get("averageScore")));
        result.setDimensionScores(toJson(structuredJson.getOrDefault("dimensionScores", Map.of())));
        result.setRiskFlags(toJson(structuredJson.getOrDefault("riskFlags", List.of())));
        result.setSuggestion(asNullableText(structuredJson.get("suggestion")));
        result.setRawResponse(response.rawResponse());
        return result;
    }

    private AiReviewResult manualRequired(Submission submission, AiReviewConfig config, Long agentRunId,
                                          String promptSnapshot, String rawResponse,
                                          String errorCode, String errorMessage) {
        AiReviewResult result = baseResult(submission, config, agentRunId, promptSnapshot);
        result.setStatus(AiReviewStatus.MANUAL_REQUIRED);
        result.setRawResponse(rawResponse);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        return result;
    }

    private AiReviewResult baseResult(Submission submission, AiReviewConfig config, Long agentRunId,
                                      String promptSnapshot) {
        AiReviewResult result = new AiReviewResult();
        result.setSubmissionId(submission.getId());
        result.setEffectiveRunId(agentRunId);
        result.setProviderId(config.getProviderId());
        result.setModelName(config.getModelName());
        result.setPromptSnapshot(promptSnapshot);
        result.setRetryCount(0);
        result.setCreatedAt(LocalDateTime.now());
        result.setUpdatedAt(LocalDateTime.now());
        return result;
    }

    private Submission loadSubmission(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(SUBMISSION_NOT_FOUND, "Submission not found");
        }
        return submission;
    }

    private AiReviewConfig loadConfig(Task task) {
        if (task == null || task.getAiReviewConfigId() == null) {
            throw new BusinessException(AI_REVIEW_CONFIG_NOT_FOUND, "AI review config not found");
        }
        AiReviewConfig config = aiReviewConfigMapper.selectById(task.getAiReviewConfigId());
        if (config == null || !task.getId().equals(config.getTaskId())) {
            throw new BusinessException(AI_REVIEW_CONFIG_NOT_FOUND, "AI review config not found");
        }
        return config;
    }

    private String buildPromptSnapshot(Submission submission, DatasetItem datasetItem, AiReviewConfig config) {
        Map<String, Object> prompt = new LinkedHashMap<>();
        prompt.put("promptTemplate", config.getPromptTemplate());
        prompt.put("scoringDimensions", parseStringList(config.getScoringDimensionsJson()));
        prompt.put("passThreshold", config.getPassThreshold());
        prompt.put("manualReviewThreshold", config.getManualReviewThreshold());
        prompt.put("outputSchema", parseObjectMap(config.getOutputSchemaJson()));
        prompt.put("itemSnapshot", datasetItem == null ? Map.of() : parseJsonValue(datasetItem.getItemJson()));
        prompt.put("answerJson", parseJsonValue(submission.getAnswerJson()));
        return toJson(prompt);
    }

    private void moveSubmissionToPendingFinal(Submission submission) {
        submission.setStatus(SubmissionStatus.PENDING_FINAL);
        submissionMapper.updateById(submission);
    }

    private void appendAudit(AiReviewResult result) {
        SystemActorContext actor = systemAgentProvider.get();
        String action = result.getStatus() == AiReviewStatus.SUCCESS
                ? "AI_REVIEW_COMPLETED"
                : "AI_REVIEW_MANUAL_REQUIRED";
        auditAppender.append(new AuditCommand(SystemActorContext.ACTOR_TYPE, actor.agentId(),
                BIZ_TYPE, result.getSubmissionId(),
                action, null, auditSnapshot(result), traceIdProvider.currentTraceId(), result.getEffectiveRunId()));
    }

    private Map<String, Object> auditSnapshot(AiReviewResult result) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("submissionId", result.getSubmissionId());
        snapshot.put("agentRunId", result.getEffectiveRunId());
        snapshot.put("status", result.getStatus());
        snapshot.put("decision", result.getDecision());
        snapshot.put("averageScore", result.getAverageScore());
        snapshot.put("errorCode", result.getErrorCode());
        snapshot.put("errorMessage", result.getErrorMessage());
        return snapshot;
    }

    private Map<String, Object> gatewayResponseSnapshot(LlmGatewayResponse response) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", response.status());
        snapshot.put("rawResponse", response.rawResponse());
        snapshot.put("contentText", response.contentText());
        snapshot.put("structuredJson", response.structuredJson());
        snapshot.put("latencyMs", response.latencyMs());
        snapshot.put("errorCode", response.errorCode());
        snapshot.put("errorMessage", response.errorMessage());
        return snapshot;
    }

    public AiReviewResultResponse toResponse(AiReviewResult result) {
        return new AiReviewResultResponse(
                result.getId(),
                result.getSubmissionId(),
                result.getEffectiveRunId(),
                result.getProviderId(),
                result.getModelName(),
                result.getStatus(),
                result.getDecision(),
                result.getAverageScore() == null ? null : result.getAverageScore().toPlainString(),
                parseObjectMapOrEmpty(result.getDimensionScores()),
                result.getRiskFlags(),
                result.getSuggestion(),
                result.getErrorCode(),
                result.getErrorMessage()
        );
    }

    private Object parseJsonValue(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(AI_REVIEW_INVALID, "AI review JSON is invalid");
        }
    }

    private List<String> parseStringList(String json) {
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(AI_REVIEW_INVALID, "AI review scoring dimensions are invalid");
        }
    }

    private Map<String, Object> parseObjectMap(String json) {
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(AI_REVIEW_INVALID, "AI review output schema is invalid");
        }
    }

    private Map<String, Object> parseObjectMapOrEmpty(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return parseObjectMap(json);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(AI_REVIEW_INVALID, "AI review payload JSON is invalid");
        }
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private String asNullableText(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
