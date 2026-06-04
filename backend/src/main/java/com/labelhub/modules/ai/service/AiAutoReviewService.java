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
import com.labelhub.infrastructure.llm.AiMetrics;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.domain.SystemActorContext;
import com.labelhub.modules.agent.service.AgentRunService;
import com.labelhub.modules.agent.service.SystemAgentProvider;
import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.domain.AiFlowAction;
import com.labelhub.modules.ai.domain.ReviewStrategy;
import com.labelhub.modules.ai.dto.AiReviewResultResponse;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.dataset.service.DatasetClaimService;
import com.labelhub.modules.review.domain.ReviewFlowStatus;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.media.service.MediaContextResolver;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AiAutoReviewService {

    private static final int SUBMISSION_NOT_FOUND = 404701;
    private static final int AI_REVIEW_CONFIG_NOT_FOUND = 404702;
    private static final int AI_REVIEW_INVALID = 400701;
    private static final int DEFAULT_MAX_RETRY = 3;
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
    private final AiReviewRetryStrategy retryStrategy;
    private final AiReviewRetryScheduler retryScheduler;
    private final SupervisorAgent supervisorAgent;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    private AiFlowDecisionService flowDecisionService;
    @Autowired
    private com.labelhub.modules.review.port.SubmissionEventPublisher eventPublisher;
    @Autowired
    private com.labelhub.modules.assignment.mapper.AssignmentMapper assignmentMapper;
    @Autowired
    private com.labelhub.modules.review.mapper.ReviewRecordMapper reviewRecordMapper;
    @Autowired
    private com.labelhub.modules.review.service.ReviewOwnershipResolver reviewOwnershipResolver;
    @Autowired(required = false)
    private DatasetClaimService datasetClaimService;
    @Autowired(required = false)
    private MediaPromptContextBuilder mediaPromptContextBuilder;
    @Autowired(required = false)
    private LlmProviderService llmProviderService;
    @Autowired(required = false)
    private MediaContextResolver mediaContextResolver;
    @Autowired
    private com.labelhub.infrastructure.redis.RedisLockService redisLockService;
    @Autowired(required = false)
    private AiMetrics aiMetrics;
    @Autowired
    private VoteAggregator voteAggregator;
    @Autowired
    private DimensionAggregator dimensionAggregator;
    @Autowired
    private PromptTemplateEngine promptTemplateEngine;

    @Autowired
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
                               TraceIdProvider traceIdProvider,
                               AiReviewRetryStrategy retryStrategy,
                               AiReviewRetryScheduler retryScheduler,
                               SupervisorAgent supervisorAgent,
                               TransactionTemplate transactionTemplate) {
        this(submissionMapper, taskMapper, datasetItemMapper, aiReviewConfigMapper, aiReviewResultMapper, rateLimiter,
                llmGateway, agentRunService, systemAgentProvider, auditAppender, traceIdProvider, new ObjectMapper(),
                retryStrategy, retryScheduler, supervisorAgent, transactionTemplate);
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
                        ObjectMapper objectMapper,
                        AiReviewRetryStrategy retryStrategy,
                        AiReviewRetryScheduler retryScheduler,
                        SupervisorAgent supervisorAgent,
                        TransactionTemplate transactionTemplate) {
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
        this.retryStrategy = retryStrategy;
        this.retryScheduler = retryScheduler;
        this.supervisorAgent = supervisorAgent;
        this.transactionTemplate = transactionTemplate;
    }

    public AiReviewResultResponse reviewSubmission(Long submissionId) {
        return reviewSubmission(submissionId, null);
    }

    public AiReviewResultResponse executeQueuedReview(Long submissionId, Long agentRunId) {
        return reviewSubmission(submissionId, agentRunId);
    }

    private AiReviewResultResponse reviewSubmission(Long submissionId, Long queuedAgentRunId) {
        String lockKey = com.labelhub.infrastructure.redis.RedisKeyBuilder.aiReviewLock(submissionId);
        if (!redisLockService.tryLock(lockKey, 0, 120_000)) {
            AiReviewResult existing = aiReviewResultMapper.selectBySubmissionId(submissionId);
            if (existing != null) {
                return toResponse(existing);
            }
            throw new BusinessException(409101, "AI review already in progress");
        }
        try {
            return doReviewSubmission(submissionId, queuedAgentRunId);
        } finally {
            redisLockService.unlock(lockKey);
        }
    }

    private AiReviewResultResponse doReviewSubmission(Long submissionId, Long queuedAgentRunId) {
        // 事务 1: 幂等检查 + 准备数据 + 创建 AgentRun
        ReviewPrepareResult prepared = transactionTemplate.execute(status -> {
            AiReviewResult existing = aiReviewResultMapper.selectBySubmissionId(submissionId);
            if (existing != null) {
                return ReviewPrepareResult.alreadyExists(existing);
            }
            Submission submission = loadSubmission(submissionId);
            Task task = taskMapper.selectById(submission.getTaskId());
            AiReviewConfig config = loadConfig(task);
            DatasetItem datasetItem = datasetItemMapper.selectById(submission.getDatasetItemId());
            MediaPromptResult prompt = buildPrompt(submission, datasetItem, config);
            AgentRun agentRun = resolveReviewAgentRun(queuedAgentRunId, submissionId, config, prompt);
            agentRunService.start(agentRun.getId());
            return ReviewPrepareResult.ready(submission, config, agentRun, prompt);
        });

        if (prepared.existing() != null) {
            return toResponse(prepared.existing());
        }

        // 无事务: LLM 远程调用 (5-30s)
        AttemptOutcome outcome;
        try {
            outcome = executeAttempt(prepared.submission(), prepared.config(),
                    prepared.agentRun(), prepared.prompt());
        } catch (Exception ex) {
            transactionTemplate.executeWithoutResult(s ->
                    agentRunService.fail(prepared.agentRun().getId(), AgentRunStatus.FAILED, ex.getMessage()));
            throw ex;
        }

        // 事务 2: 保存结果 + flowAction + 状态流转
        AiReviewResultResponse response = transactionTemplate.execute(status -> {
            AiReviewResult result;
            if (outcome.success()) {
                result = outcome.result();
                agentRunService.complete(prepared.agentRun().getId(), toJson(outcome.responseSnapshot()));
            } else {
                result = handleFailure(prepared.submission(), prepared.config(),
                        prepared.agentRun(), prepared.prompt().promptSnapshot(), outcome, 0);
            }
            if (result.getStatus() == AiReviewStatus.SUCCESS && flowDecisionService != null) {
                AiFlowAction flowAction = flowDecisionService.decide(result, prepared.config());
                result.setFlowAction(normalizeFlowAction(prepared.submission(), flowAction).name());
            }
            aiReviewResultMapper.insert(result);
            applyFlowAction(prepared.submission(), result, prepared.config());
            appendAudit(result);
            recordAiReviewMetric(prepared.config(), result, outcome.responseSnapshot());
            return toResponse(result);
        });

        publishPostTransactionEvents(prepared.submission());
        return response;
    }

    public void retryReview(Long submissionId) {
        AiReviewResult existing = aiReviewResultMapper.selectBySubmissionId(submissionId);
        if (existing == null) {
            return;
        }
        AiReviewStatus status = existing.getStatus();
        if (status != AiReviewStatus.FAILED && status != AiReviewStatus.RATE_LIMITED) {
            return;
        }

        Submission submission = loadSubmission(submissionId);
        Task task = taskMapper.selectById(submission.getTaskId());
        AiReviewConfig config = loadConfig(task);
        DatasetItem datasetItem = datasetItemMapper.selectById(submission.getDatasetItemId());
        MediaPromptResult prompt = buildPrompt(submission, datasetItem, config);
        String promptSnapshot = prompt.promptSnapshot();

        AgentRun agentRun = agentRunService.create(AGENT_TYPE, submissionId, config.getProviderId(),
                config.getModelName(), config.getPromptVersion(), promptSnapshot, null, resolveTraceId());
        agentRunService.start(agentRun.getId());

        AttemptOutcome outcome = executeAttempt(submission, config, agentRun, prompt);
        int currentRetryCount = existing.getRetryCount();

        if (outcome.success()) {
            transactionTemplate.executeWithoutResult(s -> {
                agentRunService.complete(agentRun.getId(), toJson(outcome.responseSnapshot()));
                AiReviewResult successResult = outcome.result();

                if (flowDecisionService != null) {
                    AiFlowAction flowAction = flowDecisionService.decide(successResult, config);
                    successResult.setFlowAction(normalizeFlowAction(submission, flowAction).name());
                }

                aiReviewResultMapper.updateForSuccess(submissionId,
                        AiReviewStatus.SUCCESS.name(),
                        agentRun.getId(),
                        successResult.getDecision(),
                        successResult.getAverageScore(),
                        successResult.getDimensionScores(),
                        successResult.getRiskFlags(),
                        successResult.getSuggestion(),
                        successResult.getRawResponse(),
                        successResult.getConfidence(),
                        successResult.getFlowAction(),
                        successResult.getPromptMode(),
                        successResult.getDegraded(),
                        successResult.getLimitations());
                applyFlowAction(submission, successResult, config);
                recordAiReviewMetric(config, successResult, outcome.responseSnapshot());
                appendAuditForRetrySuccess(submissionId, agentRun.getId());
            });
        } else {
            handleRetryFailure(submissionId, config, agentRun, outcome, currentRetryCount);
        }
    }

    private AttemptOutcome executeAttempt(Submission submission, AiReviewConfig config,
                                          AgentRun agentRun, MediaPromptResult prompt) {
        if ("SUPERVISOR".equals(config.getAgentMode())) {
            return executeSupervisor(submission, config, agentRun, prompt.promptSnapshot());
        }
        // 多策略分发
        ReviewStrategy strategy = resolveReviewStrategy(config);
        return switch (strategy) {
            case LIGHTWEIGHT -> executeDirect(submission, config, agentRun, prompt);
            case PARALLEL_VOTE -> executeParallelVote(submission, config, agentRun, prompt);
            case DEEP_DIMENSION -> executeDeepDimension(submission, config, agentRun, prompt);
            case AGENT_DEBATE -> executeAgentDebate(submission, config, agentRun, prompt.promptSnapshot());
        };
    }

    private ReviewStrategy resolveReviewStrategy(AiReviewConfig config) {
        if (config.getReviewStrategy() == null || config.getReviewStrategy().isBlank()) {
            return ReviewStrategy.LIGHTWEIGHT;
        }
        try {
            return ReviewStrategy.valueOf(config.getReviewStrategy());
        } catch (IllegalArgumentException e) {
            return ReviewStrategy.LIGHTWEIGHT;
        }
    }

    // ── PARALLEL_VOTE ──

    private AttemptOutcome executeParallelVote(Submission submission, AiReviewConfig config,
                                               AgentRun agentRun, MediaPromptResult prompt) {
        List<VoteModel> voteModels = parseVoteModels(config);
        if (voteModels.isEmpty()) {
            // 回退：没有配置投票模型 → 用主配置单路
            return executeDirect(submission, config, agentRun, prompt);
        }
        if (voteModels.size() == 1) {
            // 单路 → 用投票模型中的 provider/model 调用
            VoteModel vm = voteModels.get(0);
            LlmGatewayResponse response = callLlm(submission.getTaskId(), vm.providerId(), vm.modelName(),
                    prompt.messages());
            if (response.status() != LlmGatewayStatus.SUCCESS) {
                agentRunService.fail(agentRun.getId(), AgentRunStatus.FAILED, response.errorMessage());
                return AttemptOutcome.failure(response.errorCode(), response.errorMessage(), response.rawResponse());
            }
            try {
                AiReviewResult result = successResult(submission, config, agentRun.getId(), prompt, response);
                return AttemptOutcome.success(result, gatewayResponseSnapshot(response));
            } catch (BusinessException ex) {
                return AttemptOutcome.failure("INVALID_AI_REVIEW_OUTPUT", ex.getMessage(), response.rawResponse());
            }
        }

        // 2+ 路并行
        List<java.util.concurrent.CompletableFuture<Map<String, Object>>> futures = voteModels.stream()
                .map(vm -> java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    LlmGatewayResponse r = callLlm(submission.getTaskId(), vm.providerId(), vm.modelName(),
                            prompt.messages());
                    if (r.status() == LlmGatewayStatus.SUCCESS && r.structuredJson() != null) {
                        return new java.util.LinkedHashMap<>(r.structuredJson());
                    }
                    return Map.<String, Object>of("decision", "UNCERTAIN", "confidence", 0.0);
                }))
                .toList();

        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                .join();

        List<Map<String, Object>> results = futures.stream()
                .map(f -> {
                    try { return f.get(); } catch (Exception e) { return Map.<String, Object>of(); }
                })
                .filter(r -> !r.isEmpty())
                .toList();

        int minAgreement = config.getVoteMinAgreement() != null ? config.getVoteMinAgreement() : 2;
        VoteAggregator.AggregatedResult aggregated = voteAggregator.aggregate(results, minAgreement);

        try {
            Map<String, Object> enriched = new LinkedHashMap<>(aggregated.resultJson());
            AiReviewResult result = successResultFromAggregated(submission, config, agentRun.getId(),
                    prompt, enriched);
            return AttemptOutcome.success(result, enriched);
        } catch (BusinessException ex) {
            agentRunService.fail(agentRun.getId(), AgentRunStatus.MANUAL_REQUIRED, ex.getMessage());
            return AttemptOutcome.failure("INVALID_AI_REVIEW_OUTPUT", ex.getMessage(), null);
        }
    }

    // ── DEEP_DIMENSION ──

    private AttemptOutcome executeDeepDimension(Submission submission, AiReviewConfig config,
                                                 AgentRun agentRun, MediaPromptResult prompt) {
        List<String> dimensions = parseStringList(config.getScoringDimensionsJson());
        if (dimensions.isEmpty()) {
            return executeParallelVote(submission, config, agentRun, prompt);
        }

        Map<String, List<VoteModel>> dimReviewers = parseDimensionReviewers(config, dimensions);
        Map<String, List<Map<String, Object>>> dimResults = new LinkedHashMap<>();

        // 所有维度并行调用
        List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();
        for (Map.Entry<String, List<VoteModel>> entry : dimReviewers.entrySet()) {
            String dim = entry.getKey();
            List<VoteModel> reviewers = entry.getValue();
            dimResults.put(dim, java.util.Collections.synchronizedList(new ArrayList<>()));

            for (VoteModel vm : reviewers) {
                futures.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                    String dimPrompt = "Focus only on the dimension '" + dim + "'. "
                            + prompt.promptSnapshot();
                    List<LlmMessage> dimMessages = java.util.stream.Stream.concat(
                            java.util.stream.Stream.of(new LlmMessage("system",
                                    "You are LabelHub AI reviewer focused on dimension: " + dim
                                            + ". Return valid JSON only.")),
                            prompt.messages().stream()).toList();
                    LlmGatewayResponse r = callLlm(submission.getTaskId(), vm.providerId(), vm.modelName(),
                            dimMessages);
                    if (r.status() == LlmGatewayStatus.SUCCESS && r.structuredJson() != null) {
                        dimResults.get(dim).add(new LinkedHashMap<>(r.structuredJson()));
                    }
                }));
            }
        }

        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                .join();

        int minAgreement = config.getVoteMinAgreement() != null ? config.getVoteMinAgreement() : 1;
        Map<String, Object> aggregated = dimensionAggregator.aggregate(dimResults, minAgreement);

        try {
            AiReviewResult result = successResultFromAggregated(submission, config, agentRun.getId(),
                    prompt, aggregated);
            return AttemptOutcome.success(result, aggregated);
        } catch (BusinessException ex) {
            agentRunService.fail(agentRun.getId(), AgentRunStatus.MANUAL_REQUIRED, ex.getMessage());
            return AttemptOutcome.failure("INVALID_AI_REVIEW_OUTPUT", ex.getMessage(), null);
        }
    }

    // ── AGENT_DEBATE ──

    private AttemptOutcome executeAgentDebate(Submission submission, AiReviewConfig config,
                                               AgentRun agentRun, String promptSnapshot) {
        // 暂用 SupervisorAgent 承载辩论模式, 后续可扩展为多 Agent 辩论
        return executeSupervisor(submission, config, agentRun, promptSnapshot);
    }

    // ── 辅助方法 ──

    private LlmGatewayResponse callLlm(Long taskId, Long providerId, String modelName,
                                        List<LlmMessage> messages) {
        if (!rateLimiter.acquire(taskId, providerId)) {
            return new LlmGatewayResponse(LlmGatewayStatus.RATE_LIMITED, null, null, null,
                    null, "RATE_LIMITED", "AI review rate limited");
        }
        return llmGateway.review(new LlmGatewayRequest(
                providerId,
                modelName,
                messages,
                com.labelhub.infrastructure.llm.ResponseFormat.jsonSchema(AiReviewSchema.NAME, AiReviewSchema.SCHEMA)
        ));
    }

    private List<VoteModel> parseVoteModels(AiReviewConfig config) {
        if (config.getVoteModelsJson() == null || config.getVoteModelsJson().isBlank()) {
            // 回退到主配置
            if (config.getProviderId() != null && config.getModelName() != null) {
                return List.of(new VoteModel(config.getProviderId(), config.getModelName()));
            }
            return List.of();
        }
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(config.getVoteModelsJson(), new TypeReference<>() {});
            return raw.stream()
                    .map(m -> {
                        Long pid = m.get("providerId") instanceof Number n ? n.longValue() : null;
                        String mn = m.get("modelName") instanceof String s ? s : null;
                        return new VoteModel(pid, mn);
                    })
                    .filter(vm -> vm.providerId() != null && vm.modelName() != null)
                    .toList();
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private Map<String, List<VoteModel>> parseDimensionReviewers(AiReviewConfig config, List<String> dimensions) {
        List<VoteModel> defaults = parseVoteModels(config);
        if (defaults.isEmpty()) {
            return dimensions.stream().collect(java.util.stream.Collectors.toMap(d -> d, d -> List.of()));
        }
        Map<String, List<VoteModel>> result = new LinkedHashMap<>();
        if (config.getDimensionReviewersJson() != null && !config.getDimensionReviewersJson().isBlank()) {
            try {
                Map<String, Object> raw = objectMapper.readValue(config.getDimensionReviewersJson(), new TypeReference<>() {});
                for (Map.Entry<String, Object> e : raw.entrySet()) {
                    if (e.getValue() instanceof List<?> list) {
                        List<VoteModel> vms = list.stream()
                                .filter(item -> item instanceof Map<?, ?>)
                                .map(item -> {
                                    Map<?, ?> m = (Map<?, ?>) item;
                                    Long pid = m.get("providerId") instanceof Number n ? n.longValue() : null;
                                    String mn = m.get("modelName") instanceof String s ? s : null;
                                    return new VoteModel(pid, mn);
                                })
                                .filter(vm -> vm.providerId() != null && vm.modelName() != null)
                                .toList();
                        if (!vms.isEmpty()) {
                            result.put(e.getKey(), vms);
                        }
                    }
                }
            } catch (JsonProcessingException ignored) {
            }
        }
        // 未配置的维度回退到默认模型列表
        for (String dim : dimensions) {
            if (!result.containsKey(dim)) {
                result.put(dim, defaults);
            }
        }
        return result;
    }

    private AiReviewResult successResultFromAggregated(Submission submission, AiReviewConfig config,
                                                        Long agentRunId, MediaPromptResult prompt,
                                                        Map<String, Object> aggregated) {
        AiReviewResult result = baseResult(submission, config, agentRunId, prompt.promptSnapshot());
        result.setStatus(AiReviewStatus.SUCCESS);
        result.setDecision(stringValue(aggregated.get("decision"), "UNCERTAIN"));
        result.setAverageScore(aggregated.get("averageScore") instanceof Number n
                ? BigDecimal.valueOf(n.doubleValue()) : null);
        result.setDimensionScores(toJson(aggregated.get("dimensionScores")));
        result.setRiskFlags(toJson(aggregated.get("riskFlags")));
        result.setSuggestion(stringValue(aggregated.get("suggestion"), ""));
        result.setRawResponse(toJson(aggregated));
        Object confidence = aggregated.get("confidence");
        if (confidence instanceof BigDecimal d) {
            result.setConfidence(d);
        } else if (confidence instanceof Number n) {
            result.setConfidence(BigDecimal.valueOf(n.doubleValue()));
        }
        if (prompt.degraded() && result.getConfidence() != null) {
            BigDecimal penalty = config.getDegradationPenalty() != null
                    ? config.getDegradationPenalty() : BigDecimal.valueOf(0.20);
            result.setConfidence(result.getConfidence().subtract(penalty).max(BigDecimal.ZERO));
        }
        return result;
    }

    private record VoteModel(Long providerId, String modelName) {}

    private String stringValue(Object value, String fallback) {
        if (value == null) return fallback;
        String s = String.valueOf(value);
        return s.isBlank() ? fallback : s;
    }

    private double doubleValue(Object value, double fallback) {
        if (value instanceof Number n) return n.doubleValue();
        if (value == null) return fallback;
        try { return Double.parseDouble(String.valueOf(value)); } catch (NumberFormatException e) { return fallback; }
    }

    private AttemptOutcome executeDirect(Submission submission, AiReviewConfig config,
                                         AgentRun agentRun, MediaPromptResult prompt) {
        if (!rateLimiter.acquire(submission.getTaskId(), config.getProviderId())) {
            agentRunService.fail(agentRun.getId(), AgentRunStatus.RATE_LIMITED, "AI review rate limited");
            return AttemptOutcome.failure("RATE_LIMITED", "AI review rate limited", null);
        }

        LlmGatewayResponse response = llmGateway.review(new LlmGatewayRequest(
                config.getProviderId(),
                config.getModelName(),
                java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(new LlmMessage("system", "You are LabelHub AI reviewer. Return valid JSON only.")),
                        prompt.messages().stream()).toList(),
                com.labelhub.infrastructure.llm.ResponseFormat.jsonSchema(AiReviewSchema.NAME, AiReviewSchema.SCHEMA)
        ));
        if (response.status() != LlmGatewayStatus.SUCCESS) {
            agentRunService.fail(agentRun.getId(), AgentRunStatus.FAILED, response.errorMessage());
            return AttemptOutcome.failure(response.errorCode(), response.errorMessage(), response.rawResponse());
        }

        try {
            AiReviewResult result = successResult(submission, config, agentRun.getId(), prompt, response);
            return AttemptOutcome.success(result, gatewayResponseSnapshot(response));
        } catch (BusinessException ex) {
            agentRunService.fail(agentRun.getId(), AgentRunStatus.MANUAL_REQUIRED, ex.getMessage());
            return AttemptOutcome.failure("INVALID_AI_REVIEW_OUTPUT", ex.getMessage(), response.rawResponse());
        }
    }

    private AttemptOutcome executeSupervisor(Submission submission, AiReviewConfig config,
                                             AgentRun agentRun, String promptSnapshot) {
        if (!rateLimiter.acquire(submission.getTaskId(), config.getProviderId())) {
            agentRunService.fail(agentRun.getId(), AgentRunStatus.RATE_LIMITED, "AI review rate limited");
            return AttemptOutcome.failure("RATE_LIMITED", "AI review rate limited", null);
        }

        List<String> enabledTools = parseEnabledTools(config.getEnabledToolsJson());
        int maxIterations = config.getMaxIterations() != null ? config.getMaxIterations() : 10;
        DatasetItem datasetItem = datasetItemMapper.selectById(submission.getDatasetItemId());

        SupervisorRequest request = new SupervisorRequest(
                submission.getId(),
                submission.getTaskId(),
                buildSupervisorSystemPrompt(config),
                promptSnapshot,
                supervisorAgent.getToolRegistry().getToolDefinitions(enabledTools),
                new com.labelhub.modules.ai.tool.ToolContext(
                        submission.getId(), submission.getTaskId(), submission.getDatasetItemId(),
                        submission.getLabelerId(), submission.getAnswerJson(),
                        datasetItem != null ? datasetItem.getItemJson() : null),
                maxIterations,
                config.getProviderId(),
                config.getModelName()
        );

        SupervisorResult supervisorResult = supervisorAgent.execute(request);

        if (supervisorResult.success()) {
            AiReviewResult result = baseResult(submission, config, agentRun.getId(), promptSnapshot);
            result.setStatus(AiReviewStatus.SUCCESS);
            result.setDecision(supervisorResult.decision());
            result.setAverageScore(supervisorResult.averageScore());
            result.setDimensionScores(toJson(supervisorResult.dimensionScores() != null ? supervisorResult.dimensionScores() : Map.of()));
            result.setRiskFlags(toJson(supervisorResult.riskFlags() != null ? supervisorResult.riskFlags() : List.of()));
            result.setSuggestion(supervisorResult.suggestion());
            result.setRawResponse(supervisorResult.rawConversation());
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("mode", "SUPERVISOR");
            snapshot.put("result", supervisorResult);
            return AttemptOutcome.success(result, snapshot);
        } else {
            agentRunService.fail(agentRun.getId(), AgentRunStatus.FAILED, supervisorResult.errorMessage());
            return AttemptOutcome.failure(supervisorResult.errorCode(), supervisorResult.errorMessage(),
                    supervisorResult.rawConversation());
        }
    }

    private String buildSupervisorSystemPrompt(AiReviewConfig config) {
        return "You are LabelHub AI Reviewer (Supervisor mode). "
                + "You have access to tools to help you review the submission. "
                + "Use the tools to gather information, then make a final decision. "
                + "When you have enough information, respond with a JSON object containing: "
                + "decision (PASS/REJECT/UNCERTAIN), averageScore, dimensionScores, riskFlags, suggestion. "
                + "Scoring dimensions: " + config.getScoringDimensionsJson() + ". "
                + "Pass threshold: " + config.getPassThreshold() + ". "
                + "Manual review threshold: " + config.getManualReviewThreshold() + ".";
    }

    private List<String> parseEnabledTools(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (Exception e) {
            return null;
        }
    }

    private AiReviewResult handleFailure(Submission submission, AiReviewConfig config,
                                         AgentRun agentRun, String promptSnapshot,
                                         AttemptOutcome outcome, int currentRetryCount) {
        int maxRetry = config.getMaxRetry() != null ? config.getMaxRetry() : DEFAULT_MAX_RETRY;
        boolean retryable = retryStrategy.isRetryable(outcome.errorCode());
        boolean hasRetries = retryStrategy.hasRetriesRemaining(currentRetryCount, maxRetry);

        if (retryable && hasRetries) {
            boolean rateLimited = "RATE_LIMITED".equals(outcome.errorCode());
            Duration delay = retryStrategy.computeDelay(currentRetryCount, rateLimited);
            LocalDateTime nextRetryAt = LocalDateTime.now().plus(delay);

            AiReviewResult result = baseResult(submission, config, agentRun.getId(), promptSnapshot);
            result.setStatus(rateLimited ? AiReviewStatus.RATE_LIMITED : AiReviewStatus.FAILED);
            result.setRawResponse(outcome.rawResponse());
            result.setErrorCode(outcome.errorCode());
            result.setErrorMessage(outcome.errorMessage());
            result.setNextRetryAt(nextRetryAt);

            retryScheduler.scheduleRetry(submission.getId(), delay);
            return result;
        }

        return manualRequired(submission, config, agentRun.getId(), promptSnapshot,
                outcome.rawResponse(), outcome.errorCode(), outcome.errorMessage());
    }

    private void handleRetryFailure(Long submissionId, AiReviewConfig config,
                                    AgentRun agentRun, AttemptOutcome outcome, int currentRetryCount) {
        int maxRetry = config.getMaxRetry() != null ? config.getMaxRetry() : DEFAULT_MAX_RETRY;
        int newRetryCount = currentRetryCount + 1;
        boolean retryable = retryStrategy.isRetryable(outcome.errorCode());
        boolean hasRetries = retryStrategy.hasRetriesRemaining(newRetryCount, maxRetry);

        if (retryable && hasRetries) {
            boolean rateLimited = "RATE_LIMITED".equals(outcome.errorCode());
            Duration delay = retryStrategy.computeDelay(newRetryCount, rateLimited);
            LocalDateTime nextRetryAt = LocalDateTime.now().plus(delay);
            String status = rateLimited ? AiReviewStatus.RATE_LIMITED.name() : AiReviewStatus.FAILED.name();

            int updated = aiReviewResultMapper.updateForRetry(submissionId, currentRetryCount,
                    status, newRetryCount, nextRetryAt, agentRun.getId(),
                    outcome.errorCode(), outcome.errorMessage(), outcome.rawResponse());
            if (updated > 0) {
                retryScheduler.scheduleRetry(submissionId, delay);
            }
        } else {
            aiReviewResultMapper.updateForRetry(submissionId, currentRetryCount,
                    AiReviewStatus.MANUAL_REQUIRED.name(), newRetryCount, null, agentRun.getId(),
                    outcome.errorCode(), outcome.errorMessage(), outcome.rawResponse());
            appendAuditForManualRequired(submissionId, agentRun.getId());
        }
    }

    private AiReviewResult successResult(Submission submission, AiReviewConfig config, Long agentRunId,
                                         MediaPromptResult prompt, LlmGatewayResponse response) {
        Map<String, Object> structuredJson = response.structuredJson();
        if (structuredJson == null || !structuredJson.containsKey("decision")) {
            throw new BusinessException(AI_REVIEW_INVALID, "AI review decision is required");
        }
        AiReviewResult result = baseResult(submission, config, agentRunId, prompt.promptSnapshot());
        result.setStatus(AiReviewStatus.SUCCESS);
        result.setDecision(String.valueOf(structuredJson.get("decision")));
        result.setAverageScore(asBigDecimal(structuredJson.get("averageScore")));
        BigDecimal confidence = asBigDecimal(structuredJson.get("confidence"));
        if (prompt.degraded() && confidence != null) {
            BigDecimal penalty = config.getDegradationPenalty() != null
                    ? config.getDegradationPenalty() : new BigDecimal("0.20");
            confidence = confidence.subtract(penalty).max(BigDecimal.ZERO);
        }
        result.setConfidence(confidence);
        result.setDimensionScores(toJson(structuredJson.getOrDefault("dimensionScores", Map.of())));
        result.setRiskFlags(toJson(structuredJson.getOrDefault("riskFlags", List.of())));
        result.setSuggestion(asNullableText(structuredJson.get("suggestion")));
        result.setPromptMode(prompt.promptMode().name());
        result.setDegraded(prompt.degraded());
        result.setLimitations(toJson(mergeLimitations(prompt.limitations(), structuredJson.get("limitations"))));
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

    private List<String> mergeLimitations(List<String> promptLimitations, Object responseLimitations) {
        java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>(promptLimitations);
        if (responseLimitations instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    values.add(String.valueOf(item));
                }
            }
        }
        return List.copyOf(values);
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

    private MediaPromptResult buildPrompt(Submission submission, DatasetItem datasetItem, AiReviewConfig config) {
        ProviderCapability capability = ProviderCapability.textOnly();
        if (llmProviderService != null) {
            capability = llmProviderService.findEnabledById(config.getProviderId())
                    .map(llmProviderService::capability)
                    .orElse(ProviderCapability.textOnly());
        }
        MediaPromptContextBuilder builder = mediaPromptContextBuilder != null
                ? mediaPromptContextBuilder : new DefaultMediaPromptContextBuilder();
        String itemJson = datasetItem == null ? null : datasetItem.getItemJson();
        if (mediaContextResolver != null && datasetItem != null) {
            itemJson = mediaContextResolver.resolveItemJson(datasetItem.getId(), itemJson);
        }
        return builder.build(new MediaPromptInput(
                itemJson,
                submission.getAnswerJson(),
                buildPromptSnapshot(submission, datasetItem, config),
                capability,
                config.getMultimodalEnabled() == null || Boolean.TRUE.equals(config.getMultimodalEnabled()),
                config.getVisionDetail() != null ? config.getVisionDetail() : "auto",
                config.getMaxImagesPerRequest() != null ? config.getMaxImagesPerRequest() : 5
        ));
    }

    private void moveSubmissionToPendingFinal(Submission submission) {
        submission.setStatus(SubmissionStatus.PENDING_FINAL);
        submissionMapper.updateById(submission);
        reviewOwnershipResolver.assignToClaimant(submission);
    }

    private AiFlowAction normalizeFlowAction(Submission submission, AiFlowAction action) {
        if (action != AiFlowAction.AI_DIRECT_APPROVE) {
            return action;
        }
        Task task = taskMapper.selectById(submission.getTaskId());
        if (task != null && task.getOverlapCount() != null && task.getOverlapCount() > 1) {
            return AiFlowAction.AI_ASSIGN_MANUAL_REVIEW;
        }
        return action;
    }

    private void applyFlowAction(Submission submission, AiReviewResult result, AiReviewConfig config) {
        if (result.getFlowAction() == null || result.getStatus() != AiReviewStatus.SUCCESS) {
            moveSubmissionToPendingFinal(submission);
            return;
        }
        AiFlowAction action = AiFlowAction.valueOf(result.getFlowAction());
        switch (action) {
            case AI_DIRECT_APPROVE -> directApprove(submission);
            case AI_DIRECT_REJECT -> directReject(submission, result);
            default -> moveSubmissionToPendingFinal(submission);
        }
    }

    private void publishPostTransactionEvents(Submission submission) {
        if (submission.getStatus() == SubmissionStatus.APPROVED) {
            eventPublisher.publishApproved(submission.getId(), null);
        }
    }

    /**
     * Called by {@link AiReviewRecoveryRunner} at startup to replay the side effects
     * of a terminal AI review whose submission was never moved past AI_REVIEWING.
     * Mirrors {@link #applyFlowAction} but is package-private so the recovery runner
     * can delegate instead of duplicating the side-effect logic.
     */
    void applyRecoveredFlowAction(Submission submission, AiReviewResult result) {
        if (result.getFlowAction() == null || result.getStatus() != AiReviewStatus.SUCCESS) {
            moveSubmissionToPendingFinal(submission);
            return;
        }
        AiFlowAction action;
        try {
            action = AiFlowAction.valueOf(result.getFlowAction());
        } catch (IllegalArgumentException ex) {
            moveSubmissionToPendingFinal(submission);
            return;
        }
        switch (action) {
            case AI_DIRECT_APPROVE -> directApprove(submission);
            case AI_DIRECT_REJECT -> directReject(submission, result);
            default -> moveSubmissionToPendingFinal(submission);
        }
        publishPostTransactionEvents(submission);
    }

    private void directApprove(Submission submission) {
        if (submission.getStatus() != SubmissionStatus.AI_REVIEWING
                && submission.getStatus() != SubmissionStatus.PENDING_FINAL) {
            return;
        }
        submission.setStatus(SubmissionStatus.APPROVED);
        submission.setReviewFlowStatus(ReviewFlowStatus.FINAL_APPROVED.name());
        submission.setIsGolden(true);
        submissionMapper.updateById(submission);
        Assignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
        if (assignment != null) {
            assignment.setStatus(AssignmentStatus.APPROVED);
            assignment.setApprovedAt(LocalDateTime.now());
            assignmentMapper.updateById(assignment);
        }
        if (datasetClaimService != null) {
            datasetClaimService.increaseApprovedCount(submission.getDatasetItemId());
        }
    }

    private void directReject(Submission submission, AiReviewResult result) {
        if (submission.getStatus() != SubmissionStatus.AI_REVIEWING
                && submission.getStatus() != SubmissionStatus.PENDING_FINAL) {
            return;
        }
        submission.setStatus(SubmissionStatus.REJECTED);
        submission.setReviewFlowStatus(ReviewFlowStatus.REJECTED.name());
        submissionMapper.updateById(submission);
        Assignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
        if (assignment != null) {
            assignment.setStatus(AssignmentStatus.RETURNED);
            assignment.setReturnedAt(LocalDateTime.now());
            assignmentMapper.updateById(assignment);
        }
        if (eventPublisher != null) {
            eventPublisher.publishRejected(submission.getId(), null, "AI direct reject");
        }
        if (reviewRecordMapper != null) {
            com.labelhub.modules.review.domain.ReviewRecord record = new com.labelhub.modules.review.domain.ReviewRecord();
            record.setSubmissionId(submission.getId());
            record.setReviewerId(null);
            record.setAction(com.labelhub.modules.review.domain.ReviewAction.AI_DIRECT_REJECT);
            record.setReviewLevel(0);
            record.setReason(result.getSuggestion());
            record.setCreatedAt(LocalDateTime.now());
            reviewRecordMapper.insert(record);
        }
    }

    private void appendAudit(AiReviewResult result) {
        if (result.getStatus() == AiReviewStatus.FAILED || result.getStatus() == AiReviewStatus.RATE_LIMITED) {
            return;
        }
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
        snapshot.put("flowAction", result.getFlowAction());
        snapshot.put("averageScore", result.getAverageScore());
        snapshot.put("confidence", result.getConfidence());
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
                result.getConfidence() == null ? null : result.getConfidence().toPlainString(),
                result.getFlowAction(),
                result.getPromptMode(),
                result.getDegraded(),
                safeParseLimitations(result.getLimitations()),
                result.getErrorCode(),
                result.getErrorMessage(),
                result.getCreatedAt(),
                result.getUpdatedAt()
        );
    }

    private AgentRun resolveReviewAgentRun(Long queuedAgentRunId, Long submissionId,
                                           AiReviewConfig config, MediaPromptResult prompt) {
        if (queuedAgentRunId != null) {
            java.util.Optional<AgentRun> pendingRun = agentRunService.findPending(queuedAgentRunId);
            if (pendingRun.isPresent()) {
                return pendingRun.get();
            }
        }
        return agentRunService.create(AGENT_TYPE, submissionId, config.getProviderId(),
                config.getModelName(), config.getPromptVersion(), prompt.promptSnapshot(), null, resolveTraceId());
    }

    private void recordAiReviewMetric(AiReviewConfig config, AiReviewResult result, Map<String, Object> responseSnapshot) {
        if (aiMetrics == null || config == null || result == null) {
            return;
        }
        Long latencyMs = null;
        if (responseSnapshot != null && responseSnapshot.get("latencyMs") instanceof Number number) {
            latencyMs = number.longValue();
        }
        aiMetrics.record("AI_REVIEW", config.getProviderId(), config.getModelName(),
                result.getStatus() == null ? "UNKNOWN" : result.getStatus().name(),
                result.getErrorCode(), latencyMs);
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

    private List<String> safeParseLimitations(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
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

    private void appendAuditForRetrySuccess(Long submissionId, Long agentRunId) {
        SystemActorContext actor = systemAgentProvider.get();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("submissionId", submissionId);
        snapshot.put("agentRunId", agentRunId);
        snapshot.put("status", AiReviewStatus.SUCCESS);
        auditAppender.append(new AuditCommand(SystemActorContext.ACTOR_TYPE, actor.agentId(),
                BIZ_TYPE, submissionId,
                "AI_REVIEW_COMPLETED", null, snapshot, resolveTraceId(), agentRunId));
    }

    private void appendAuditForManualRequired(Long submissionId, Long agentRunId) {
        SystemActorContext actor = systemAgentProvider.get();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("submissionId", submissionId);
        snapshot.put("agentRunId", agentRunId);
        snapshot.put("status", AiReviewStatus.MANUAL_REQUIRED);
        auditAppender.append(new AuditCommand(SystemActorContext.ACTOR_TYPE, actor.agentId(),
                BIZ_TYPE, submissionId,
                "AI_REVIEW_MANUAL_REQUIRED", null, snapshot, resolveTraceId(), agentRunId));
    }

    private String resolveTraceId() {
        String traceId = traceIdProvider.currentTraceId();
        return traceId != null ? traceId : "retry-" + UUID.randomUUID();
    }

    record AttemptOutcome(boolean success, AiReviewResult result, Map<String, Object> responseSnapshot,
                          String errorCode, String errorMessage, String rawResponse) {

        static AttemptOutcome success(AiReviewResult result, Map<String, Object> responseSnapshot) {
            return new AttemptOutcome(true, result, responseSnapshot, null, null, null);
        }

        static AttemptOutcome failure(String errorCode, String errorMessage, String rawResponse) {
            return new AttemptOutcome(false, null, null, errorCode, errorMessage, rawResponse);
        }
    }

    private record ReviewPrepareResult(
            AiReviewResult existing,
            Submission submission,
            AiReviewConfig config,
            AgentRun agentRun,
            MediaPromptResult prompt) {

        static ReviewPrepareResult alreadyExists(AiReviewResult existing) {
            return new ReviewPrepareResult(existing, null, null, null, null);
        }

        static ReviewPrepareResult ready(Submission submission, AiReviewConfig config,
                                         AgentRun agentRun, MediaPromptResult prompt) {
            return new ReviewPrepareResult(null, submission, config, agentRun, prompt);
        }
    }
}
