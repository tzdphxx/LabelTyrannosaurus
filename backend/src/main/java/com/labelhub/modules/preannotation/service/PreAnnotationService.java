package com.labelhub.modules.preannotation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.RoleCode;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.infrastructure.llm.LlmGateway;
import com.labelhub.infrastructure.llm.LlmGatewayRequest;
import com.labelhub.infrastructure.llm.LlmGatewayResponse;
import com.labelhub.infrastructure.llm.LlmGatewayStatus;
import com.labelhub.infrastructure.llm.LlmMessage;
import com.labelhub.infrastructure.llm.AiMetrics;
import com.labelhub.infrastructure.llmtask.LlmTaskClaimResult;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueMessage;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueRecord;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueService;
import com.labelhub.infrastructure.llmtask.LlmTaskType;
import com.labelhub.infrastructure.redis.RedisLockService;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.service.AgentRunService;
import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.domain.LlmProvider;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.ai.service.MediaPromptContextBuilder;
import com.labelhub.modules.ai.service.MediaPromptInput;
import com.labelhub.modules.ai.service.MediaPromptResult;
import com.labelhub.modules.ai.service.PromptTemplateEngine;
import com.labelhub.modules.ai.service.ProviderCapability;
import com.labelhub.modules.ai.service.LlmProviderService;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.media.service.MediaContextResolver;
import com.labelhub.modules.preannotation.domain.PreAnnotation;
import com.labelhub.modules.preannotation.domain.PreAnnotationStatus;
import com.labelhub.modules.preannotation.dto.PreAnnotationRunRequest;
import com.labelhub.modules.preannotation.dto.PreAnnotationResponse;
import com.labelhub.modules.preannotation.mapper.PreAnnotationMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.template.domain.TemplateVersion;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreAnnotationService {

    private static final int FORBIDDEN = 403801;
    private static final int NOT_FOUND = 404801;
    private static final int CONFIG_NOT_FOUND = 404802;
    private static final int INVALID_REQUEST = 400801;
    private static final String AGENT_TYPE = "PRE_ANNOTATION";
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> MAP_LIST = new TypeReference<>() {};
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final AssignmentMapper assignmentMapper;
    private final TaskMapper taskMapper;
    private final DatasetItemMapper datasetItemMapper;
    @SuppressWarnings("unused")
    private final TemplateVersionMapper templateVersionMapper;
    private final AiReviewConfigMapper aiReviewConfigMapper;
    private final LlmProviderService llmProviderService;
    private final LlmGateway llmGateway;
    private final AgentRunService agentRunService;
    private final PreAnnotationMapper preAnnotationMapper;
    private final SubmissionMapper submissionMapper;
    private final AuditAppender auditAppender;
    private final TraceIdProvider traceIdProvider;
    private final MediaPromptContextBuilder mediaPromptContextBuilder;
    private final RedisLockService redisLockService;
    private final LlmTaskQueueService llmTaskQueueService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired(required = false)
    private MediaContextResolver mediaContextResolver;
    @Autowired(required = false)
    private AiMetrics aiMetrics;
    @Autowired
    private PromptTemplateEngine promptTemplateEngine;

    @Autowired
    public PreAnnotationService(AssignmentMapper assignmentMapper,
                                TaskMapper taskMapper,
                                DatasetItemMapper datasetItemMapper,
                                TemplateVersionMapper templateVersionMapper,
                                AiReviewConfigMapper aiReviewConfigMapper,
                                LlmProviderService llmProviderService,
                                LlmGateway llmGateway,
                                AgentRunService agentRunService,
                                PreAnnotationMapper preAnnotationMapper,
                                SubmissionMapper submissionMapper,
                                AuditAppender auditAppender,
                                TraceIdProvider traceIdProvider,
                                MediaPromptContextBuilder mediaPromptContextBuilder,
                                RedisLockService redisLockService,
                                LlmTaskQueueService llmTaskQueueService) {
        this.assignmentMapper = assignmentMapper;
        this.taskMapper = taskMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.templateVersionMapper = templateVersionMapper;
        this.aiReviewConfigMapper = aiReviewConfigMapper;
        this.llmProviderService = llmProviderService;
        this.llmGateway = llmGateway;
        this.agentRunService = agentRunService;
        this.preAnnotationMapper = preAnnotationMapper;
        this.submissionMapper = submissionMapper;
        this.auditAppender = auditAppender;
        this.traceIdProvider = traceIdProvider;
        this.mediaPromptContextBuilder = mediaPromptContextBuilder;
        this.redisLockService = redisLockService;
        this.llmTaskQueueService = llmTaskQueueService;
    }

    public PreAnnotationService(AssignmentMapper assignmentMapper,
                                TaskMapper taskMapper,
                                DatasetItemMapper datasetItemMapper,
                                TemplateVersionMapper templateVersionMapper,
                                AiReviewConfigMapper aiReviewConfigMapper,
                                LlmProviderService llmProviderService,
                                LlmGateway llmGateway,
                                AgentRunService agentRunService,
                                PreAnnotationMapper preAnnotationMapper,
                                SubmissionMapper submissionMapper,
                                AuditAppender auditAppender,
                                TraceIdProvider traceIdProvider,
                                MediaPromptContextBuilder mediaPromptContextBuilder,
                                RedisLockService redisLockService) {
        this(assignmentMapper, taskMapper, datasetItemMapper, templateVersionMapper, aiReviewConfigMapper,
                llmProviderService, llmGateway, agentRunService, preAnnotationMapper, submissionMapper,
                auditAppender, traceIdProvider, mediaPromptContextBuilder, redisLockService,
                new NoOpLlmTaskQueueService());
    }

    public PreAnnotationResponse run(Long assignmentId, Long labelerId) {
        return run(assignmentId, labelerId, null);
    }

    public PreAnnotationResponse run(Long assignmentId, Long labelerId, PreAnnotationRunRequest request) {
        Assignment assignment = assignmentMapper.selectOwnedAssignment(assignmentId, labelerId);
        if (assignment == null) {
            throw new BusinessException(FORBIDDEN, "Forbidden");
        }
        validateRequest(assignment, request);

        String lockKey = "lock:preannotation:" + assignmentId;
        return redisLockService.withLock(lockKey, 2000L, 120000L, () -> {
            PreAnnotation running = preAnnotationMapper.selectRunningByAssignmentId(assignmentId);
            if (running != null) {
                return toResponse(running, false);
            }
            return doRun(assignment, request);
        });
    }

    private PreAnnotationResponse doRun(Assignment assignment, PreAnnotationRunRequest request) {
        Long labelerId = assignment.getLabelerId();
        Task task = taskMapper.selectById(assignment.getTaskId());
        AiReviewConfig config = loadConfig(task);
        Long datasetItemId = request != null && request.datasetItemId() != null
                ? request.datasetItemId() : assignment.getDatasetItemId();
        Long templateVersionId = request != null && request.templateVersionId() != null
                ? request.templateVersionId() : assignment.getTemplateVersionId();
        String currentAnswerJson = request != null && request.currentAnswerJson() != null
                ? request.currentAnswerJson() : assignment.getDraftAnswerJson();
        DatasetItem item = datasetItemMapper.selectById(datasetItemId);
        String itemJson = resolveItemJson(datasetItemId, item);
        LlmProvider provider = loadProvider(config.getProviderId());
        ProviderCapability capability = llmProviderService.capability(provider);
        MediaPromptResult prompt = mediaPromptContextBuilder.build(new MediaPromptInput(
                itemJson,
                currentAnswerJson,
                preAnnotationPrompt(config, task, currentAnswerJson),
                capability,
                config.getMultimodalEnabled() == null || Boolean.TRUE.equals(config.getMultimodalEnabled()),
                config.getVisionDetail() != null ? config.getVisionDetail() : "auto",
                config.getMaxImagesPerRequest() != null ? config.getMaxImagesPerRequest() : 5
        ));
        PreAnnotation record = baseRecord(assignment, datasetItemId, prompt);
        record.setStatus(PreAnnotationStatus.PENDING);
        preAnnotationMapper.insert(record);

        String traceId = traceIdProvider.currentTraceId();
        AgentRun run = agentRunService.create(AGENT_TYPE, null, config.getProviderId(), config.getModelName(),
                config.getPromptVersion(), prompt.promptSnapshot(), assignment.getId(), traceId);
        record.setAgentRunId(run.getId());
        record.setStatus(PreAnnotationStatus.RUNNING);
        record.setUpdatedAt(LocalDateTime.now());
        preAnnotationMapper.updateById(record);
        agentRunService.start(run.getId());
        llmTaskQueueService.enqueue(new LlmTaskQueueMessage(
                LlmTaskType.PRE_ANNOTATION,
                record.getId(),
                assignment.getTaskId(),
                assignment.getId(),
                null,
                record.getId(),
                null,
                run.getId(),
                traceId,
                0,
                Instant.now()
        ));
        appendAudit(labelerId, task, record);
        return toResponse(record, false);
    }

    public void executeQueuedPreAnnotation(Long preAnnotationId) {
        PreAnnotation record = preAnnotationMapper.selectById(preAnnotationId);
        if (record == null || isTerminal(record.getStatus())) {
            return;
        }
        Assignment assignment = assignmentMapper.selectById(record.getAssignmentId());
        if (assignment == null) {
            return;
        }
        Long labelerId = assignment.getLabelerId();
        Task task = taskMapper.selectById(assignment.getTaskId());
        AiReviewConfig config = loadConfig(task);
        Long datasetItemId = record.getDatasetItemId();
        Long templateVersionId = assignment.getTemplateVersionId();
        String currentAnswerJson = assignment.getDraftAnswerJson();
        DatasetItem item = datasetItemMapper.selectById(datasetItemId);
        String itemJson = resolveItemJson(datasetItemId, item);
        LlmProvider provider = loadProvider(config.getProviderId());
        ProviderCapability capability = llmProviderService.capability(provider);
        MediaPromptResult prompt = mediaPromptContextBuilder.build(new MediaPromptInput(
                itemJson,
                currentAnswerJson,
                preAnnotationPrompt(config, task, currentAnswerJson),
                capability,
                config.getMultimodalEnabled() == null || Boolean.TRUE.equals(config.getMultimodalEnabled()),
                config.getVisionDetail() != null ? config.getVisionDetail() : "auto",
                config.getMaxImagesPerRequest() != null ? config.getMaxImagesPerRequest() : 5
        ));
        ensureExecutablePreAnnotationRun(record, config, prompt.promptSnapshot(), assignment.getId());

        LlmGatewayResponse gatewayResponse;
        try {
            gatewayResponse = llmGateway.review(new LlmGatewayRequest(
                    config.getProviderId(),
                    config.getModelName(),
                    withSystemPrompt(prompt.messages())
            ));
        } catch (Exception llmEx) {
            agentRunService.fail(record.getAgentRunId(), AgentRunStatus.FAILED,
                    "LLM call exception: " + llmEx.getMessage());
            record.setStatus(PreAnnotationStatus.FAILED);
            record.setErrorCode("LLM_EXCEPTION");
            record.setErrorMessage(llmEx.getMessage());
            record.setUpdatedAt(LocalDateTime.now());
            preAnnotationMapper.updateById(record);
            appendAudit(labelerId, task, record);
            recordPreAnnotationMetric(config, record, null);
            return;
        }
        if (gatewayResponse.status() == LlmGatewayStatus.SUCCESS) {
            if (hasRequiredOutput(gatewayResponse.structuredJson())) {
                agentRunService.complete(record.getAgentRunId(), toJson(gatewayResponseSnapshot(gatewayResponse)));
                fillSuccess(record, gatewayResponse, prompt, templateVersionId, config);
            } else {
                agentRunService.fail(record.getAgentRunId(), AgentRunStatus.MANUAL_REQUIRED,
                        "Pre-annotation output is incomplete");
                record.setStatus(PreAnnotationStatus.MANUAL_REQUIRED);
                record.setErrorCode("MISSING_PRE_ANNOTATION_OUTPUT");
                record.setErrorMessage("Pre-annotation output is incomplete");
                record.setRawResponse(gatewayResponse.rawResponse());
            }
        } else {
            AgentRunStatus runStatus = gatewayResponse.status() == LlmGatewayStatus.RATE_LIMITED
                    ? AgentRunStatus.RATE_LIMITED : AgentRunStatus.FAILED;
            agentRunService.fail(record.getAgentRunId(), runStatus, gatewayResponse.errorMessage());
            record.setStatus(gatewayResponse.status() == LlmGatewayStatus.RATE_LIMITED
                    ? PreAnnotationStatus.RATE_LIMITED : PreAnnotationStatus.FAILED);
            record.setErrorCode(gatewayResponse.errorCode());
            record.setErrorMessage(gatewayResponse.errorMessage());
            record.setRawResponse(gatewayResponse.rawResponse());
        }
        record.setUpdatedAt(LocalDateTime.now());
        try {
            preAnnotationMapper.updateById(record);
        } catch (Exception persistEx) {
            if (record.getStatus() == PreAnnotationStatus.SUCCESS) {
                agentRunService.fail(record.getAgentRunId(), AgentRunStatus.FAILED,
                        "PreAnnotation persist failed: " + persistEx.getMessage());
            }
            throw persistEx;
        }
        recordPreAnnotationMetric(config, record, gatewayResponse.latencyMs());
        appendAudit(labelerId, task, record);
    }

    private boolean isTerminal(PreAnnotationStatus status) {
        return status == PreAnnotationStatus.SUCCESS
                || status == PreAnnotationStatus.FAILED
                || status == PreAnnotationStatus.RATE_LIMITED
                || status == PreAnnotationStatus.MANUAL_REQUIRED;
    }

    private void ensureExecutablePreAnnotationRun(PreAnnotation record, AiReviewConfig config,
                                                  String promptSnapshot, Long assignmentId) {
        java.util.Optional<AgentRun> activeRun = agentRunService.findActive(record.getAgentRunId());
        if (activeRun.isPresent()) {
            if (activeRun.get().getStatus() == AgentRunStatus.PENDING) {
                agentRunService.start(activeRun.get().getId());
            }
            return;
        }
        AgentRun run = agentRunService.create(AGENT_TYPE, null, config.getProviderId(), config.getModelName(),
                config.getPromptVersion(), promptSnapshot, assignmentId, traceIdProvider.currentTraceId());
        record.setAgentRunId(run.getId());
        record.setStatus(PreAnnotationStatus.RUNNING);
        record.setUpdatedAt(LocalDateTime.now());
        preAnnotationMapper.updateById(record);
        agentRunService.start(run.getId());
    }

    public PreAnnotationResponse latest(Long assignmentId, Long labelerId) {
        Assignment assignment = assignmentMapper.selectOwnedAssignment(assignmentId, labelerId);
        if (assignment == null) {
            throw new BusinessException(FORBIDDEN, "Forbidden");
        }
        PreAnnotation record = preAnnotationMapper.selectLatestByAssignmentId(assignmentId);
        return record == null ? null : toResponse(record, false);
    }

    public PreAnnotationResponse getDetail(Long preAnnotationId, CurrentUser currentUser) {
        PreAnnotation record = preAnnotationMapper.selectById(preAnnotationId);
        if (record == null) {
            throw new BusinessException(NOT_FOUND, "Pre-annotation not found");
        }
        Assignment assignment = assignmentMapper.selectById(record.getAssignmentId());
        Task task = taskMapper.selectById(record.getTaskId());
        boolean includeDiff = requireAccess(record, assignment, task, currentUser);
        return toResponse(record, includeDiff);
    }

    private boolean requireAccess(PreAnnotation record, Assignment assignment, Task task, CurrentUser currentUser) {
        if (currentUser.roles().contains(RoleCode.LABELER)
                && assignment != null
                && currentUser.userId().equals(assignment.getLabelerId())) {
            return false;
        }
        if (currentUser.roles().contains(RoleCode.OWNER)
                && task != null
                && currentUser.userId().equals(task.getOwnerId())) {
            return true;
        }
        if (currentUser.roles().contains(RoleCode.REVIEWER)) {
            return true;
        }
        throw new BusinessException(FORBIDDEN, "Forbidden");
    }

    private AiReviewConfig loadConfig(Task task) {
        if (task == null || task.getAiReviewConfigId() == null) {
            throw new BusinessException(CONFIG_NOT_FOUND, "AI review config not found");
        }
        AiReviewConfig config = aiReviewConfigMapper.selectById(task.getAiReviewConfigId());
        if (config == null || !task.getId().equals(config.getTaskId())) {
            throw new BusinessException(CONFIG_NOT_FOUND, "AI review config not found");
        }
        return config;
    }

    private LlmProvider loadProvider(Long providerId) {
        Optional<LlmProvider> provider = llmProviderService.findEnabledById(providerId);
        if (provider.isEmpty()) {
            throw new BusinessException(CONFIG_NOT_FOUND, "Enabled LLM provider is required");
        }
        return provider.get();
    }

    private void validateRequest(Assignment assignment, PreAnnotationRunRequest request) {
        if (request == null) {
            return;
        }
        if (request.mode() != null && !request.mode().isBlank()
                && !"SUGGEST_ONLY".equals(request.mode())) {
            throw new BusinessException(INVALID_REQUEST, "Pre-annotation mode must be SUGGEST_ONLY");
        }
        if (request.templateVersionId() != null
                && !request.templateVersionId().equals(assignment.getTemplateVersionId())) {
            throw new BusinessException(INVALID_REQUEST, "Template version does not match assignment");
        }
        if (request.datasetItemId() != null
                && !request.datasetItemId().equals(assignment.getDatasetItemId())) {
            throw new BusinessException(INVALID_REQUEST, "Dataset item does not match assignment");
        }
    }

    private String preAnnotationPrompt(AiReviewConfig config, Task task, String currentAnswerJson) {
        PromptTemplateEngine.TaskPromptContext ctx = new PromptTemplateEngine.TaskPromptContext(
                task.getTitle(),
                task.getDescription(),
                task.getInstructionRichText(),
                config.getScoringDimensionsJson(),
                config.getPassThreshold() != null ? config.getPassThreshold().toString() : "-",
                config.getManualReviewThreshold() != null ? config.getManualReviewThreshold().toString() : "-",
                config.getPromptVersion()
        );
        String userTemplate = config.getPromptTemplate() != null ? config.getPromptTemplate() : "";
        return promptTemplateEngine.buildPreAnnotationPrompt(userTemplate, ctx, List.of(), currentAnswerJson);
    }

    private List<LlmMessage> withSystemPrompt(List<LlmMessage> messages) {
        return java.util.stream.Stream.concat(
                java.util.stream.Stream.of(new LlmMessage("system", "Return valid JSON only.")),
                messages.stream()).toList();
    }

    private String resolveItemJson(Long datasetItemId, DatasetItem item) {
        String itemJson = item == null ? null : item.getItemJson();
        if (mediaContextResolver != null) {
            return mediaContextResolver.resolveItemJson(datasetItemId, itemJson);
        }
        return itemJson;
    }

    private PreAnnotation baseRecord(Assignment assignment, Long datasetItemId, MediaPromptResult prompt) {
        PreAnnotation record = new PreAnnotation();
        record.setAssignmentId(assignment.getId());
        record.setTaskId(assignment.getTaskId());
        record.setDatasetItemId(datasetItemId);
        record.setLabelerId(assignment.getLabelerId());
        record.setPromptMode(prompt.promptMode().name());
        record.setDegraded(prompt.degraded());
        record.setLimitations(toJson(prompt.limitations()));
        record.setIgnoredFieldsJson(toJson(List.of()));
        record.setMediaUnderstandingJson(toJson(prompt.mediaUnderstanding()));
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        return record;
    }

    private boolean hasRequiredOutput(Map<String, Object> json) {
        return json != null
                && json.containsKey("suggestedAnswerJson")
                && json.containsKey("fieldSuggestions")
                && json.containsKey("riskFlags")
                && json.containsKey("overallConfidence")
                && json.containsKey("limitations");
    }

    private void fillSuccess(PreAnnotation record, LlmGatewayResponse response, MediaPromptResult prompt,
                             Long templateVersionId, AiReviewConfig config) {
        Map<String, Object> json = response.structuredJson() != null ? response.structuredJson() : Map.of();
        FieldFilterResult filtered = filterOutput(templateVersionId,
                asMap(json.get("suggestedAnswerJson")),
                asMapList(json.get("fieldSuggestions")));
        record.setStatus(PreAnnotationStatus.SUCCESS);
        record.setSuggestedAnswerJson(toJson(filtered.suggestedAnswerJson()));
        record.setFieldSuggestions(toJson(filtered.fieldSuggestions()));
        record.setRiskFlags(toJson(json.getOrDefault("riskFlags", List.of())));
        BigDecimal confidence = asBigDecimal(json.get("overallConfidence"));
        if (prompt.degraded() && confidence != null) {
            BigDecimal penalty = config.getDegradationPenalty() != null
                    ? config.getDegradationPenalty() : new BigDecimal("0.20");
            confidence = confidence.subtract(penalty).max(BigDecimal.ZERO);
        }
        record.setOverallConfidence(confidence);
        record.setLimitations(toJson(mergeLimitations(prompt.limitations(), json.get("limitations"))));
        record.setIgnoredFieldsJson(toJson(filtered.ignoredFields()));
        record.setMediaUnderstandingJson(toJson(prompt.mediaUnderstanding()));
        record.setRawResponse(response.rawResponse());
    }

    private FieldFilterResult filterOutput(Long templateVersionId,
                                           Map<String, Object> suggestedAnswerJson,
                                           List<Map<String, Object>> fieldSuggestions) {
        AllowedFields allowedFields = allowedFields(templateVersionId);
        if (allowedFields.allowed().isEmpty()) {
            return new FieldFilterResult(suggestedAnswerJson, fieldSuggestions, List.of());
        }
        Map<String, Object> filteredAnswer = new LinkedHashMap<>();
        LinkedHashSet<String> ignored = new LinkedHashSet<>();
        suggestedAnswerJson.forEach((field, value) -> {
            if (allowedFields.allowed().contains(field)) {
                filteredAnswer.put(field, value);
            } else {
                ignored.add(field);
            }
        });
        List<Map<String, Object>> filteredSuggestions = new ArrayList<>();
        for (Map<String, Object> suggestion : fieldSuggestions) {
            String field = suggestion.get("field") == null ? null : String.valueOf(suggestion.get("field"));
            if (field != null && allowedFields.allowed().contains(field)) {
                filteredSuggestions.add(suggestion);
            } else if (field != null) {
                ignored.add(field);
            }
        }
        ignored.addAll(allowedFields.blocked());
        return new FieldFilterResult(filteredAnswer, filteredSuggestions, List.copyOf(ignored));
    }

    private AllowedFields allowedFields(Long templateVersionId) {
        TemplateVersion templateVersion = templateVersionMapper.selectById(templateVersionId);
        if (templateVersion == null || templateVersion.getSchemaJson() == null
                || templateVersion.getSchemaJson().isBlank()) {
            return new AllowedFields(Set.of(), Set.of());
        }
        try {
            JsonNode root = objectMapper.readTree(templateVersion.getSchemaJson());
            LinkedHashSet<String> allowed = new LinkedHashSet<>();
            LinkedHashSet<String> blocked = new LinkedHashSet<>();
            collectFields(root, allowed, blocked);
            return new AllowedFields(allowed, blocked);
        } catch (Exception ex) {
            return new AllowedFields(Set.of(), Set.of());
        }
    }

    private void collectFields(JsonNode node, Set<String> allowed, Set<String> blocked) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            JsonNode fieldNode = node.get("field");
            if (fieldNode != null && fieldNode.isTextual()) {
                String field = fieldNode.asText();
                String type = firstText(node, "type", "componentType", "component");
                if ("ShowItem".equalsIgnoreCase(type)) {
                    blocked.add(field);
                } else {
                    allowed.add(field);
                }
            }
            node.fields().forEachRemaining(entry -> collectFields(entry.getValue(), allowed, blocked));
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectFields(child, allowed, blocked));
        }
    }

    private String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && value.isTextual()) {
                return value.asText();
            }
        }
        return null;
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

    private void appendAudit(Long actorId, Task task, PreAnnotation record) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("preAnnotationId", record.getId());
        snapshot.put("assignmentId", record.getAssignmentId());
        snapshot.put("agentRunId", record.getAgentRunId());
        snapshot.put("status", record.getStatus());
        snapshot.put("promptMode", record.getPromptMode());
        snapshot.put("degraded", record.getDegraded());
        snapshot.put("limitations", parseStringList(record.getLimitations()));
        auditAppender.append(new AuditCommand("USER", actorId, "PRE_ANNOTATION",
                record.getId() != null ? record.getId() : record.getAssignmentId(),
                "PRE_ANNOTATION_RUN", null, snapshot, traceIdProvider.currentTraceId(), record.getAgentRunId()));
    }

    private void recordPreAnnotationMetric(AiReviewConfig config, PreAnnotation record, Long latencyMs) {
        if (aiMetrics != null && config != null && record != null) {
            aiMetrics.record("PRE_ANNOTATION", config.getProviderId(), config.getModelName(),
                    record.getStatus() == null ? "UNKNOWN" : record.getStatus().name(),
                    record.getErrorCode(), latencyMs);
        }
    }

    private PreAnnotationResponse toResponse(PreAnnotation record, boolean includeDiff) {
        return new PreAnnotationResponse(
                record.getId(),
                record.getAssignmentId(),
                record.getAgentRunId(),
                record.getStatus(),
                parseMap(record.getSuggestedAnswerJson()),
                parseMapList(record.getFieldSuggestions()),
                parseStringList(record.getRiskFlags()),
                record.getOverallConfidence(),
                parseStringList(record.getLimitations()),
                record.getPromptMode(),
                record.getDegraded(),
                parseStringList(record.getIgnoredFieldsJson()),
                parseMap(record.getMediaUnderstandingJson()),
                includeDiff ? finalDiff(record) : null,
                record.getErrorCode(),
                record.getErrorMessage(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    private Map<String, Object> finalDiff(PreAnnotation record) {
        Submission latest = submissionMapper.selectLatestActiveByAssignmentId(record.getAssignmentId());
        if (latest == null) {
            return Map.of();
        }
        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("suggestedAnswerJson", parseMap(record.getSuggestedAnswerJson()));
        diff.put("finalAnswerJson", parseMap(latest.getAnswerJson()));
        return diff;
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

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> parseMapList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, MAP_LIST);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asMapList(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    private record AllowedFields(Set<String> allowed, Set<String> blocked) {
    }

    private record FieldFilterResult(Map<String, Object> suggestedAnswerJson,
                                     List<Map<String, Object>> fieldSuggestions,
                                     List<String> ignoredFields) {
    }

    private static class NoOpLlmTaskQueueService implements LlmTaskQueueService {
        @Override
        public String enqueue(LlmTaskQueueMessage message) {
            return "";
        }

        @Override
        public List<LlmTaskQueueRecord> read(LlmTaskType taskType, String consumerName, int count, Duration waitTime) {
            return List.of();
        }

        @Override
        public LlmTaskClaimResult claimStale(LlmTaskType taskType, String consumerName, Duration minIdleTime,
                                             String startMessageId, int count) {
            return new LlmTaskClaimResult("0-0", List.of());
        }

        @Override
        public boolean ack(LlmTaskType taskType, String messageId) {
            return true;
        }
    }
}
