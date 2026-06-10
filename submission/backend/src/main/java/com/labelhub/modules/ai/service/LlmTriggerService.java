package com.labelhub.modules.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.RoleCode;
import com.labelhub.common.web.TraceIdProvider;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.labelhub.infrastructure.llm.LlmGateway;
import com.labelhub.infrastructure.llm.LlmGatewayRequest;
import com.labelhub.infrastructure.llm.LlmGatewayResponse;
import com.labelhub.infrastructure.llm.LlmGatewayStatus;
import com.labelhub.infrastructure.llm.LlmMessage;
import com.labelhub.infrastructure.llm.AiMetrics;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueMessage;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueService;
import com.labelhub.infrastructure.llmtask.LlmTaskStatus;
import com.labelhub.infrastructure.llmtask.LlmTaskType;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.service.AgentRunService;
import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.domain.LlmTriggerRun;
import com.labelhub.modules.ai.dto.LlmTriggerRunRequest;
import com.labelhub.modules.ai.dto.LlmTriggerRunResponse;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.ai.mapper.LlmTriggerRunMapper;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.media.service.MediaContextResolver;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.template.domain.TemplateVersion;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LlmTriggerService {

    private static final Logger log = LoggerFactory.getLogger(LlmTriggerService.class);

    private static final int FORBIDDEN = 403001;
    private static final int TASK_NOT_FOUND = 404001;
    private static final int LLM_TRIGGER_INVALID = 400501;
    private static final int DATASET_ITEM_NOT_FOUND = 404502;
    private static final int LLM_TRIGGER_PROVIDER_UNAVAILABLE = 400503;
    private static final int RUN_NOT_FOUND = 404503;
    private static final int ASSIGNMENT_NOT_FOUND = 404504;
    private static final int TEMPLATE_VERSION_NOT_FOUND = 404203;
    private static final String BIZ_TYPE = "LLM_TRIGGER";
    private static final String USER_ACTOR_TYPE = "USER";
    private static final String AGENT_TYPE = "LLM_TRIGGER";
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final TaskMapper taskMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final AssignmentMapper assignmentMapper;
    private final LlmProviderService llmProviderService;
    private final LlmTriggerRateLimiter rateLimiter;
    private final LlmGateway llmGateway;
    private final AgentRunService agentRunService;
    private final AuditAppender auditAppender;
    private final TraceIdProvider traceIdProvider;
    private final LlmTaskQueueService llmTaskQueueService;
    private final LlmTriggerAsyncExecutor llmTriggerAsyncExecutor;
    private final LlmTriggerRunMapper llmTriggerRunMapper;
    private final TemplateVersionMapper templateVersionMapper;
    private final AiReviewConfigMapper aiReviewConfigMapper;
    private final ObjectMapper objectMapper;
    private final boolean immediateExecution;
    @Autowired(required = false)
    private MediaContextResolver mediaContextResolver;
    @Autowired(required = false)
    private MediaPromptContextBuilder mediaPromptContextBuilder;
    @Autowired(required = false)
    private VideoKeyFrameService videoKeyFrameService;
    @Autowired(required = false)
    private AiMetrics aiMetrics;
    @Autowired
    private PromptTemplateEngine promptTemplateEngine;

    @Autowired
    public LlmTriggerService(TaskMapper taskMapper,
                             DatasetItemMapper datasetItemMapper,
                             AssignmentMapper assignmentMapper,
                             LlmProviderService llmProviderService,
                             LlmTriggerRateLimiter rateLimiter,
                             LlmGateway llmGateway,
                             AgentRunService agentRunService,
                             AuditAppender auditAppender,
                             TraceIdProvider traceIdProvider,
                             LlmTaskQueueService llmTaskQueueService,
                             LlmTriggerAsyncExecutor llmTriggerAsyncExecutor,
                             LlmTriggerRunMapper llmTriggerRunMapper,
                             TemplateVersionMapper templateVersionMapper,
                             AiReviewConfigMapper aiReviewConfigMapper) {
        this(taskMapper, datasetItemMapper, assignmentMapper, llmProviderService, rateLimiter,
                llmGateway, agentRunService, auditAppender, traceIdProvider, llmTaskQueueService, llmTriggerAsyncExecutor,
                llmTriggerRunMapper, templateVersionMapper, aiReviewConfigMapper, new ObjectMapper(), false);
    }

    public LlmTriggerService(TaskMapper taskMapper,
                             DatasetItemMapper datasetItemMapper,
                             AssignmentMapper assignmentMapper,
                             LlmProviderService llmProviderService,
                             LlmTriggerRateLimiter rateLimiter,
                             LlmGateway llmGateway,
                             AgentRunService agentRunService,
                             AuditAppender auditAppender,
                             TraceIdProvider traceIdProvider) {
        this(taskMapper, datasetItemMapper, assignmentMapper, llmProviderService, rateLimiter,
                llmGateway, agentRunService, auditAppender, traceIdProvider, null, null,
                null, null, null, new ObjectMapper(), true);
    }

    LlmTriggerService(TaskMapper taskMapper,
                      DatasetItemMapper datasetItemMapper,
                      AssignmentMapper assignmentMapper,
                      LlmProviderService llmProviderService,
                      LlmTriggerRateLimiter rateLimiter,
                      LlmGateway llmGateway,
                      AgentRunService agentRunService,
                      AuditAppender auditAppender,
                      TraceIdProvider traceIdProvider,
                      LlmTaskQueueService llmTaskQueueService,
                      LlmTriggerRunMapper llmTriggerRunMapper,
                      TemplateVersionMapper templateVersionMapper,
                      AiReviewConfigMapper aiReviewConfigMapper,
                      ObjectMapper objectMapper) {
        this(taskMapper, datasetItemMapper, assignmentMapper, llmProviderService, rateLimiter,
                llmGateway, agentRunService, auditAppender, traceIdProvider, llmTaskQueueService,
                null, llmTriggerRunMapper, templateVersionMapper, aiReviewConfigMapper, objectMapper, false);
    }

    LlmTriggerService(TaskMapper taskMapper,
                      DatasetItemMapper datasetItemMapper,
                      AssignmentMapper assignmentMapper,
                      LlmProviderService llmProviderService,
                      LlmTriggerRateLimiter rateLimiter,
                      LlmGateway llmGateway,
                      AgentRunService agentRunService,
                      AuditAppender auditAppender,
                      TraceIdProvider traceIdProvider,
                      LlmTaskQueueService llmTaskQueueService,
                      LlmTriggerAsyncExecutor llmTriggerAsyncExecutor,
                      LlmTriggerRunMapper llmTriggerRunMapper,
                      TemplateVersionMapper templateVersionMapper,
                      AiReviewConfigMapper aiReviewConfigMapper,
                      ObjectMapper objectMapper) {
        this(taskMapper, datasetItemMapper, assignmentMapper, llmProviderService, rateLimiter,
                llmGateway, agentRunService, auditAppender, traceIdProvider, llmTaskQueueService,
                llmTriggerAsyncExecutor, llmTriggerRunMapper, templateVersionMapper, aiReviewConfigMapper,
                objectMapper, false);
    }

    private LlmTriggerService(TaskMapper taskMapper,
                              DatasetItemMapper datasetItemMapper,
                              AssignmentMapper assignmentMapper,
                              LlmProviderService llmProviderService,
                              LlmTriggerRateLimiter rateLimiter,
                              LlmGateway llmGateway,
                              AgentRunService agentRunService,
                              AuditAppender auditAppender,
                              TraceIdProvider traceIdProvider,
                              LlmTaskQueueService llmTaskQueueService,
                              LlmTriggerAsyncExecutor llmTriggerAsyncExecutor,
                              LlmTriggerRunMapper llmTriggerRunMapper,
                              TemplateVersionMapper templateVersionMapper,
                              AiReviewConfigMapper aiReviewConfigMapper,
                              ObjectMapper objectMapper,
                              boolean immediateExecution) {
        this.taskMapper = taskMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.assignmentMapper = assignmentMapper;
        this.llmProviderService = llmProviderService;
        this.rateLimiter = rateLimiter;
        this.llmGateway = llmGateway;
        this.agentRunService = agentRunService;
        this.auditAppender = auditAppender;
        this.traceIdProvider = traceIdProvider;
        this.llmTaskQueueService = llmTaskQueueService;
        this.llmTriggerAsyncExecutor = llmTriggerAsyncExecutor;
        this.llmTriggerRunMapper = llmTriggerRunMapper;
        this.templateVersionMapper = templateVersionMapper;
        this.aiReviewConfigMapper = aiReviewConfigMapper;
        this.objectMapper = objectMapper;
        this.immediateExecution = immediateExecution;
    }

    // ── Labeler 从 assignment 触发 ──

    public LlmTriggerRunResponse runForAssignment(CurrentUser currentUser, Long assignmentId,
                                                   LlmTriggerRunRequest request) {
        if (!currentUser.hasRole(RoleCode.LABELER)) {
            throw new BusinessException(FORBIDDEN, "只有标注员可以在作答页触发 LLM 辅助");
        }
        Assignment assignment = currentUser.isAdmin()
                ? assignmentMapper.selectById(assignmentId)
                : assignmentMapper.selectOwnedAssignment(assignmentId, currentUser.userId());
        if (assignment == null) {
            throw new BusinessException(ASSIGNMENT_NOT_FOUND, "领取记录不存在");
        }
        Task task = loadTask(assignment.getTaskId());
        DatasetItem datasetItem = loadDatasetItem(assignment.getDatasetItemId(), task.getId());
        TemplateVersion templateVersion = loadTemplateVersion(assignment.getTemplateVersionId());
        AiReviewConfig config = loadTaskAiReviewConfig(task);
        TemplateContext templateContext = resolveTemplateContext(templateVersion, request.componentId());
        requireEnabledProvider(config.getProviderId());

        return doRun(task, assignment.getTemplateVersionId(), datasetItem, config, templateContext,
                request, currentUser.userId(), assignmentId);
    }

    // ── Owner 预览测试 ──

    public LlmTriggerRunResponse testFromTask(CurrentUser currentUser, Long taskId,
                                               LlmTriggerRunRequest request) {
        if (!currentUser.hasRole(RoleCode.OWNER)) {
            throw new BusinessException(FORBIDDEN, "只有任务负责人可以测试 LLM 触发器");
        }
        Task task = loadTask(taskId);
        if (!currentUser.isAdmin() && !currentUser.userId().equals(task.getOwnerId())) {
            throw new BusinessException(FORBIDDEN, "当前账号不是任务负责人");
        }
        DatasetItem datasetItem = loadDatasetItem(request.datasetItemId(), task.getId());
        TemplateVersion templateVersion = loadTemplateVersion(task.getPublishedTemplateVersionId());
        AiReviewConfig config = loadTaskAiReviewConfig(task);
        TemplateContext templateContext = resolveTemplateContext(templateVersion, request.componentId());
        requireEnabledProvider(config.getProviderId());

        return doRun(task, task.getPublishedTemplateVersionId(), datasetItem, config, templateContext,
                request, currentUser.userId(), null);
    }

    // ── 公共执行逻辑 ──

    private LlmTriggerRunResponse doRun(Task task, Long templateVersionId,
                                          DatasetItem datasetItem, AiReviewConfig config,
                                          TemplateContext templateContext, LlmTriggerRunRequest request,
                                          Long actorId, Long assignmentId) {
        String traceId = traceIdProvider.currentTraceId();
        Map<String, Object> inputSnapshot = buildInputSnapshot(task, datasetItem, config, templateContext, request, traceId);
        AgentRun agentRun = agentRunService.create(AGENT_TYPE, null, config.getProviderId(),
                config.getModelName().trim(),
                resolveTriggerPromptVersion(config),
                toJson(inputSnapshot), assignmentId, traceId);
        agentRunService.start(agentRun.getId());

        LlmTriggerRun run = new LlmTriggerRun();
        run.setTaskId(task.getId());
        run.setAssignmentId(assignmentId);
        run.setTemplateVersionId(templateVersionId);
        run.setDatasetItemId(datasetItem != null ? datasetItem.getId() : request.datasetItemId());
        run.setComponentId(templateContext.componentId() == null ? null : String.valueOf(templateContext.componentId()));
        run.setProviderId(config.getProviderId());
        run.setModelName(config.getModelName().trim());
        run.setAgentRunId(agentRun.getId());
        run.setStatus(LlmTaskStatus.RUNNING.name());
        run.setTargetFieldsJson(toJson(templateContext.targetFields()));
        run.setInputSnapshotJson(toJson(inputSnapshot));
        run.setCreatedBy(actorId);
        run.setCreatedAt(LocalDateTime.now());
        run.setUpdatedAt(LocalDateTime.now());
        if (llmTriggerRunMapper != null) {
            llmTriggerRunMapper.insert(run);
        } else {
            run.setId(agentRun.getId());
        }

        if (immediateExecution) {
            executeRun(run, task, run.getProviderId(), run.getModelName());
        } else {
            llmTriggerAsyncExecutor.submit(run.getId(), traceId);
            /*
             * Redis Stream implementation retained for rollback/reference:
             *
             * llmTaskQueueService.enqueue(new LlmTaskQueueMessage(
             *         LlmTaskType.LLM_TRIGGER,
             *         run.getId(),
             *         task.getId(),
             *         assignmentId,
             *         null,
             *         null,
             *         run.getId(),
             *         agentRun.getId(),
             *         traceId,
             *         0,
             *         Instant.now()
             * ));
             */
        }

        appendAudit(actorId, task.getId(), request, run);
        return toRunResponse(run);
    }

    public LlmTriggerRunResponse getRun(CurrentUser currentUser, Long triggerRunId) {
        LlmTriggerRun run = llmTriggerRunMapper.selectById(triggerRunId);
        if (run == null) {
            throw new BusinessException(RUN_NOT_FOUND, "LLM 触发运行记录不存在");
        }
        Task task = loadTask(run.getTaskId());
        if (currentUser.isAdmin()) {
            return toRunResponse(run);
        }
        if (currentUser.hasRole(RoleCode.OWNER) && currentUser.userId().equals(task.getOwnerId())) {
            return toRunResponse(run);
        }
        if (run.getAssignmentId() != null && currentUser.hasRole(RoleCode.LABELER)) {
            Assignment assignment = assignmentMapper.selectOwnedAssignment(run.getAssignmentId(), currentUser.userId());
            if (assignment != null) {
                return toRunResponse(run);
            }
        }
        throw new BusinessException(FORBIDDEN, "无权查看 LLM 触发运行记录");
    }

    public void executeQueuedTrigger(Long triggerRunId) {
        LlmTriggerRun run = llmTriggerRunMapper.selectById(triggerRunId);
        if (run == null || LlmTaskStatus.SUCCESS.name().equals(run.getStatus())) {
            return;
        }
        Task task = loadTask(run.getTaskId());
        executeRun(run, task, run.getProviderId(), run.getModelName());
    }

    public void failQueuedTrigger(Long triggerRunId, String errorCode, String errorMessage) {
        if (llmTriggerRunMapper == null) {
            return;
        }
        LlmTriggerRun run = llmTriggerRunMapper.selectById(triggerRunId);
        if (run == null || isTerminalStatus(run.getStatus())) {
            return;
        }
        agentRunService.fail(run.getAgentRunId(), AgentRunStatus.FAILED, safeErrorMessage(errorMessage));
        fillFailure(run, LlmTaskStatus.FAILED.name(), errorCode, safeErrorMessage(errorMessage));
        recordLlmTriggerMetric(run);
    }

    private void executeRun(LlmTriggerRun run, Task task, Long providerId, String modelName) {
        if (!rateLimiter.acquire(run.getTaskId(), run.getCreatedBy(), providerId)) {
            agentRunService.fail(run.getAgentRunId(), AgentRunStatus.RATE_LIMITED, "LLM trigger rate limited");
            fillFailure(run, LlmTaskStatus.RATE_LIMITED.name(), "RATE_LIMITED", "LLM trigger rate limited");
            recordLlmTriggerMetric(run);
            appendAudit(run.getCreatedBy(), task.getId(), null, run);
            return;
        }

        AiReviewConfig config = loadTaskAiReviewConfig(task);
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
        TemplateVersion templateVersion = loadTemplateVersion(run.getTemplateVersionId());
        String systemPrompt = promptTemplateEngine.buildLlmTriggerPrompt(userTemplate, ctx,
                longValue(run.getComponentId()), parseStringList(run.getTargetFieldsJson()),
                extractSchemaFields(templateVersion.getSchemaJson()), run.getInputSnapshotJson());

        TriggerPrompt triggerPrompt = buildTriggerPrompt(systemPrompt, run, config, providerId);
        LlmGatewayResponse gatewayResponse;
        try {
            gatewayResponse = llmGateway.review(new LlmGatewayRequest(
                    providerId,
                    modelName,
                    triggerPrompt.messages()
            ));
            if (shouldFallbackVideoDirect(triggerPrompt, gatewayResponse)) {
                TriggerPrompt keyFramePrompt = buildTriggerFallbackPrompt(systemPrompt, run, config, providerId, false);
                if (keyFramePrompt != null && keyFramePrompt.promptMode() == PromptMode.VIDEO_KEYFRAMES) {
                    gatewayResponse = llmGateway.review(new LlmGatewayRequest(providerId, modelName,
                            keyFramePrompt.messages()));
                    triggerPrompt = keyFramePrompt;
                }
                if (gatewayResponse.status() != LlmGatewayStatus.SUCCESS) {
                    TriggerPrompt textPrompt = buildTriggerFallbackPrompt(systemPrompt, run, config, providerId, true);
                    if (textPrompt != null && textPrompt.promptMode() == PromptMode.TEXT_ONLY) {
                        gatewayResponse = llmGateway.review(new LlmGatewayRequest(providerId, modelName,
                                textPrompt.messages()));
                        triggerPrompt = textPrompt;
                    }
                }
            }
        } catch (Exception ex) {
            agentRunService.fail(run.getAgentRunId(), AgentRunStatus.FAILED, safeErrorMessage(ex.getMessage()));
            fillFailure(run, LlmTaskStatus.FAILED.name(), "LLM_EXCEPTION", safeErrorMessage(ex.getMessage()));
            recordLlmTriggerMetric(run);
            appendAudit(run.getCreatedBy(), task.getId(), null, run);
            return;
        }
        if (gatewayResponse.status() == LlmGatewayStatus.SUCCESS) {
            Map<String, Object> normalizedResult = normalizeStructuredPatch(
                    gatewayResponse.structuredJson(), longValue(run.getComponentId()), parseStringList(run.getTargetFieldsJson()));
            Map<String, Object> outputSnapshot = gatewayResponseSnapshot(gatewayResponse);
            outputSnapshot.put("normalizedResult", normalizedResult);
            agentRunService.complete(run.getAgentRunId(), toJson(outputSnapshot));
            run.setStatus(LlmTaskStatus.SUCCESS.name());
            run.setResultJson(toJson(normalizedResult));
            run.setContentText(stringValue(normalizedResult.get("displayText"), gatewayResponse.contentText()));
            run.setLatencyMs(gatewayResponse.latencyMs());
            run.setErrorCode(null);
            run.setErrorMessage(null);
        } else {
            AgentRunStatus runStatus = gatewayResponse.status() == LlmGatewayStatus.RATE_LIMITED
                    ? AgentRunStatus.RATE_LIMITED : AgentRunStatus.FAILED;
            agentRunService.fail(run.getAgentRunId(), runStatus, gatewayResponse.errorMessage());
            fillFailure(run,
                    gatewayResponse.status() == LlmGatewayStatus.RATE_LIMITED
                            ? LlmTaskStatus.RATE_LIMITED.name() : LlmTaskStatus.FAILED.name(),
                    gatewayResponse.errorCode(), gatewayResponse.errorMessage());
            run.setContentText(gatewayResponse.contentText());
            run.setLatencyMs(gatewayResponse.latencyMs());
        }
        run.setUpdatedAt(LocalDateTime.now());
        if (llmTriggerRunMapper != null) {
            llmTriggerRunMapper.updateById(run);
        }
        recordLlmTriggerMetric(run);
        appendAudit(run.getCreatedBy(), task.getId(), null, run);
    }

    private TriggerPrompt buildTriggerPrompt(String systemPrompt, LlmTriggerRun run,
                                             AiReviewConfig config, Long providerId) {
        List<LlmMessage> textMessages = List.of(
                new LlmMessage("system", systemPrompt),
                new LlmMessage("user", run.getInputSnapshotJson())
        );
        Map<String, Object> input = parseObjectMapOrEmpty(run.getInputSnapshotJson());
        Object itemSnapshot = input.get("itemSnapshot");
        if (!(itemSnapshot instanceof Map<?, ?> itemMap)) {
            return new TriggerPrompt(textMessages, PromptMode.TEXT_ONLY);
        }
        return buildTriggerPromptFromItem(systemPrompt, run, config, providerId, input, castObjectMap(itemMap));
    }

    private TriggerPrompt buildTriggerPromptFromItem(String systemPrompt, LlmTriggerRun run,
                                                     AiReviewConfig config, Long providerId,
                                                     Map<String, Object> input, Map<String, Object> item) {
        List<LlmMessage> textMessages = List.of(
                new LlmMessage("system", systemPrompt),
                new LlmMessage("user", run.getInputSnapshotJson())
        );
        String mediaType = stringValue(item.get("media_type"), "");
        if (mediaType == null || mediaType.isBlank() || "text".equalsIgnoreCase(mediaType)) {
            return new TriggerPrompt(textMessages, PromptMode.TEXT_ONLY);
        }
        ProviderCapability capability = llmProviderService.findEnabledById(providerId)
                .map(llmProviderService::capability)
                .orElse(ProviderCapability.textOnly());
        MediaPromptContextBuilder builder = mediaPromptContextBuilder != null
                ? mediaPromptContextBuilder : new DefaultMediaPromptContextBuilder();
        MediaPromptResult prompt = builder.build(new MediaPromptInput(
                toJson(item),
                toJson(input.getOrDefault("currentAnswerJson", Map.of())),
                run.getInputSnapshotJson(),
                capability,
                config.getMultimodalEnabled() == null || Boolean.TRUE.equals(config.getMultimodalEnabled()),
                config.getVisionDetail() != null ? config.getVisionDetail() : "auto",
                config.getMaxImagesPerRequest() != null ? config.getMaxImagesPerRequest() : 5
        ));
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", systemPrompt));
        messages.addAll(prompt.messages());
        return new TriggerPrompt(messages, prompt.promptMode());
    }

    private TriggerPrompt buildTriggerFallbackPrompt(String systemPrompt, LlmTriggerRun run,
                                                     AiReviewConfig config, Long providerId, boolean textOnly) {
        Map<String, Object> input = parseObjectMapOrEmpty(run.getInputSnapshotJson());
        Object itemSnapshot = input.get("itemSnapshot");
        if (!(itemSnapshot instanceof Map<?, ?> itemMap)) {
            return null;
        }
        Map<String, Object> item = new LinkedHashMap<>(castObjectMap(itemMap));
        if (!"video".equalsIgnoreCase(stringValue(item.get("media_type"), ""))) {
            return null;
        }
        String mediaUrl = stringValue(item.get("media_url"), "");
        if (textOnly) {
            item.remove("media_url");
            item.remove("key_frame_urls");
            addMediaLimitation(item, "VIDEO_MEDIA_FALLBACK_TEXT_ONLY");
        } else {
            List<String> keyFrameUrls = stringListValue(item.get("key_frame_urls"));
            if (keyFrameUrls.isEmpty() && videoKeyFrameService != null) {
                keyFrameUrls = videoKeyFrameService.generateKeyFrameUrls(mediaUrl, intValue(item.get("video_duration_seconds")));
            }
            if (keyFrameUrls.isEmpty()) {
                return null;
            }
            item.put("key_frame_urls", keyFrameUrls);
            item.remove("media_url");
            addMediaLimitation(item, "VIDEO_DIRECT_FALLBACK_TO_KEYFRAMES");
        }
        return buildTriggerPromptFromItem(systemPrompt, run, config, providerId, input, item);
    }

    private boolean shouldFallbackVideoDirect(TriggerPrompt triggerPrompt, LlmGatewayResponse response) {
        return triggerPrompt != null
                && triggerPrompt.promptMode() == PromptMode.VIDEO_DIRECT
                && response != null
                && response.status() != LlmGatewayStatus.SUCCESS
                && response.status() != LlmGatewayStatus.RATE_LIMITED;
    }

    private Task loadTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(TASK_NOT_FOUND, "任务不存在");
        }
        return task;
    }

    private void requireEnabledProvider(Long providerId) {
        if (llmProviderService.findEnabledById(providerId).isEmpty()) {
            throw new BusinessException(LLM_TRIGGER_PROVIDER_UNAVAILABLE, "需要配置已启用的 LLM Provider");
        }
    }

    private DatasetItem loadDatasetItem(Long datasetItemId, Long taskId) {
        if (datasetItemId == null) {
            return null;
        }
        DatasetItem datasetItem = datasetItemMapper.selectById(datasetItemId);
        if (datasetItem == null || !taskId.equals(datasetItem.getTaskId())) {
            throw new BusinessException(DATASET_ITEM_NOT_FOUND, "数据项不存在");
        }
        return datasetItem;
    }

    private TemplateVersion loadTemplateVersion(Long templateVersionId) {
        if (templateVersionId == null || templateVersionMapper == null) {
            throw new BusinessException(TEMPLATE_VERSION_NOT_FOUND, "Template version not found");
        }
        TemplateVersion templateVersion = templateVersionMapper.selectById(templateVersionId);
        if (templateVersion == null) {
            throw new BusinessException(TEMPLATE_VERSION_NOT_FOUND, "Template version not found");
        }
        return templateVersion;
    }

    private AiReviewConfig loadTaskAiReviewConfig(Task task) {
        if (aiReviewConfigMapper == null) {
            throw new BusinessException(LLM_TRIGGER_INVALID, "LlmTrigger requires task AI review config");
        }
        AiReviewConfig config = null;
        if (task.getAiReviewConfigId() != null) {
            config = aiReviewConfigMapper.selectById(task.getAiReviewConfigId());
            if (config != null && !task.getId().equals(config.getTaskId())) {
                config = null;
            }
        }
        if (config == null) {
            List<AiReviewConfig> configs = aiReviewConfigMapper.selectList(
                    new QueryWrapper<AiReviewConfig>().eq("task_id", task.getId()));
            config = configs.isEmpty() ? null : configs.get(0);
            if (configs.size() > 1) {
                log.warn("Multiple AiReviewConfig rows for task {} ({} rows); using id={}",
                        task.getId(), configs.size(), config.getId());
            }
        }
        if (config == null || config.getProviderId() == null
                || config.getModelName() == null || config.getModelName().isBlank()) {
            throw new BusinessException(LLM_TRIGGER_INVALID, "LlmTrigger requires task AI review config");
        }
        return config;
    }

    private TemplateContext resolveTemplateContext(TemplateVersion templateVersion, Long requestedComponentId) {
        List<PromptTemplateEngine.SchemaField> schemaFields = extractSchemaFields(templateVersion.getSchemaJson());
        List<String> targetFields = schemaFields.stream()
                .filter(field -> !field.showOnly())
                .map(PromptTemplateEngine.SchemaField::field)
                .filter(field -> field != null && !field.isBlank())
                .distinct()
                .toList();
        Long componentId = requestedComponentId != null ? requestedComponentId : templateVersion.getTemplateId();
        return new TemplateContext(componentId, schemaFields, targetFields);
    }

    private Map<String, Object> buildInputSnapshot(Task task, DatasetItem datasetItem, AiReviewConfig config,
                                                    TemplateContext templateContext, LlmTriggerRunRequest request,
                                                    String traceId) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("traceId", traceId);
        input.put("task", Map.of(
                "taskId", task.getId(),
                "title", task.getTitle() == null ? "" : task.getTitle(),
                "description", task.getDescription() == null ? "" : task.getDescription(),
                "instructionRichText", task.getInstructionRichText() == null ? "" : task.getInstructionRichText()
        ));
        input.put("componentId", templateContext.componentId());
        input.put("templateFields", templateContext.schemaFields());
        input.put("targetFields", templateContext.targetFields());
        input.put("scoringDimensions", parseStringList(config.getScoringDimensionsJson()));
        Map<String, Object> thresholds = new LinkedHashMap<>();
        thresholds.put("passThreshold", config.getPassThreshold());
        thresholds.put("manualReviewThreshold", config.getManualReviewThreshold());
        input.put("thresholds", thresholds);
        input.put("promptVersion", config.getPromptVersion());
        input.put("userInstruction", request.userInstruction() == null ? "" : request.userInstruction().trim());
        String itemJson = datasetItem == null ? null : datasetItem.getItemJson();
        if (mediaContextResolver != null && datasetItem != null) {
            itemJson = mediaContextResolver.resolveItemJson(datasetItem.getId(), itemJson);
        }
        input.put("itemSnapshot", datasetItem == null ? Map.of() : parseJsonValue(itemJson));
        input.put("currentAnswerJson", request.currentAnswerJson() == null ? Map.of() : request.currentAnswerJson());
        input.put("llmInstruction", buildLlmTriggerPrompt());
        return input;
    }

    private String buildLlmTriggerPrompt() {
        return "Use the task, itemSnapshot, templateFields, currentAnswerJson and scoringDimensions to produce a JSON "
                + "object that can be merged into the answer. Return exactly: componentId, targetFields, patch, "
                + "displayText, confidence, reasoningSummary, warnings. Include only schema targetFields in patch.";
    }

    public LlmTriggerRunResponse toRunResponse(LlmTriggerRun run) {
        Map<String, Object> result = parseObjectMapOrEmpty(run.getResultJson());
        Map<String, Object> input = parseObjectMapOrEmpty(run.getInputSnapshotJson());
        return new LlmTriggerRunResponse(
                run.getId(),
                run.getAgentRunId(),
                longValue(run.getComponentId()),
                result,
                objectMapValue(result.get("patch")),
                stringValue(result.get("displayText"), run.getContentText()),
                parseStringList(run.getTargetFieldsJson()),
                run.getContentText(),
                decimalValue(result.get("confidence")),
                parseWarnings(result.get("warnings")),
                stringValue(input.get("traceId"), null),
                run.getStatus(),
                run.getLatencyMs(),
                run.getErrorCode(),
                run.getErrorMessage()
        );
    }

    private void fillFailure(LlmTriggerRun run, String status, String errorCode, String errorMessage) {
        run.setStatus(status);
        run.setErrorCode(errorCode);
        run.setErrorMessage(errorMessage);
        run.setUpdatedAt(LocalDateTime.now());
        if (llmTriggerRunMapper != null) {
            llmTriggerRunMapper.updateById(run);
        }
    }

    private boolean isTerminalStatus(String status) {
        return LlmTaskStatus.SUCCESS.name().equals(status)
                || LlmTaskStatus.FAILED.name().equals(status)
                || LlmTaskStatus.RATE_LIMITED.name().equals(status)
                || LlmTaskStatus.MANUAL_REQUIRED.name().equals(status);
    }

    private String safeErrorMessage(String message) {
        return message == null || message.isBlank() ? "LLM task failed" : message;
    }

    private void recordLlmTriggerMetric(LlmTriggerRun run) {
        if (aiMetrics != null && run != null) {
            aiMetrics.record("LLM_TRIGGER", run.getProviderId(), run.getModelName(),
                    run.getStatus(), run.getErrorCode(), run.getLatencyMs());
        }
    }

    private void appendAudit(Long actorId, Long taskId, LlmTriggerRunRequest request, LlmTriggerRun run) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("taskId", taskId);
        snapshot.put("triggerRunId", run.getId());
        snapshot.put("providerId", run.getProviderId());
        snapshot.put("modelName", run.getModelName());
        snapshot.put("targetFields", parseStringList(run.getTargetFieldsJson()));
        snapshot.put("status", run.getStatus());
        snapshot.put("errorCode", run.getErrorCode());
        snapshot.put("errorMessage", run.getErrorMessage());
        auditAppender.append(new AuditCommand(USER_ACTOR_TYPE, actorId, BIZ_TYPE, taskId, "LLM_TRIGGER_RUN",
                null, snapshot, traceIdProvider.currentTraceId(), run.getAgentRunId()));
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

    private Map<String, Object> normalizeStructuredPatch(Map<String, Object> structuredJson,
                                                          Long componentId,
                                                          List<String> targetFields) {
        Map<String, Object> source = structuredJson == null ? Map.of() : structuredJson;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("componentId", componentId);
        result.put("targetFields", targetFields);
        List<String> warnings = new ArrayList<>(parseWarnings(source.get("warnings")));
        Map<String, Object> sourcePatch;
        if (source.get("patch") instanceof Map<?, ?> map) {
            sourcePatch = castObjectMap(map);
        } else {
            log.warn("LLM trigger response missing 'patch' field for template componentId {}; "
                    + "using entire response as patch source", componentId);
            sourcePatch = source;
            warnings.add("LLM response missing 'patch' wrapper; results may be incomplete");
        }
        Map<String, Object> patch = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : sourcePatch.entrySet()) {
            if (targetFields.isEmpty() || targetFields.contains(entry.getKey())) {
                patch.put(entry.getKey(), entry.getValue());
            } else if (!List.of("componentId", "targetFields", "displayText", "confidence",
                    "reasoningSummary", "warnings", "patch").contains(entry.getKey())) {
                warnings.add("Dropped non-schema patch field: " + entry.getKey());
            }
        }
        result.put("patch", patch);
        result.put("displayText", stringValue(source.get("displayText"), null));
        result.put("confidence", decimalValue(source.get("confidence")));
        result.put("reasoningSummary", stringValue(source.get("reasoningSummary"), null));
        result.put("warnings", warnings);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castObjectMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        return result;
    }

    private List<PromptTemplateEngine.SchemaField> extractSchemaFields(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(schemaJson);
            List<PromptTemplateEngine.SchemaField> fields = new ArrayList<>();
            collectSchemaFields(root, fields, new LinkedHashSet<>());
            return fields;
        } catch (Exception ex) {
            log.warn("Failed to extract LlmTrigger schema fields: {}", ex.getMessage());
            return List.of();
        }
    }

    private void collectSchemaFields(JsonNode node, List<PromptTemplateEngine.SchemaField> fields, Set<String> seen) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            JsonNode fieldNode = node.get("field");
            if (fieldNode != null && fieldNode.isTextual()) {
                String field = fieldNode.asText();
                if (!field.isBlank() && seen.add(field)) {
                    String type = firstText(node, "type", "componentType", "component");
                    boolean showOnly = "ShowItem".equalsIgnoreCase(type);
                    fields.add(new PromptTemplateEngine.SchemaField(
                            field,
                            type == null ? "" : type,
                            firstTextOrDefault(node, field, "label", "title", "name"),
                            optionValues(node.get("options")),
                            node.path("required").asBoolean(false),
                            showOnly,
                            firstTextOrDefault(node, "", "description", "help", "placeholder")));
                }
            }
            node.fields().forEachRemaining(entry -> collectSchemaFields(entry.getValue(), fields, seen));
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectSchemaFields(child, fields, seen));
        }
    }

    private List<String> optionValues(JsonNode options) {
        if (options == null || !options.isArray()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        options.forEach(option -> {
            String value = option.isTextual() ? option.asText() : firstText(option, "label", "value", "name");
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        });
        return values;
    }

    private String firstTextOrDefault(JsonNode node, String fallback, String... keys) {
        String value = firstText(node, keys);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String firstText(JsonNode node, String... keys) {
        if (node == null) {
            return null;
        }
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && value.isTextual()) {
                return value.asText();
            }
        }
        return null;
    }

    private Object parseJsonValue(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(LLM_TRIGGER_INVALID, "数据项 JSON 格式不合法");
        }
    }

    private Map<String, Object> parseObjectMapOrEmpty(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private Map<String, Object> objectMapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return castObjectMap(map);
        }
        return Map.of();
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private Long longValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<String> parseWarnings(Object value) {
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        return list.stream()
                .map(item -> stringValue(item, null))
                .filter(item -> item != null && !item.isBlank())
                .toList();
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void addMediaLimitation(Map<String, Object> item, String limitation) {
        List<String> limitations = new ArrayList<>(stringListValue(item.get("media_context_limitations")));
        if (!limitations.contains(limitation)) {
            limitations.add(limitation);
        }
        item.put("media_context_limitations", limitations);
    }

    private List<String> stringListValue(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (Object item : iterable) {
            if (item != null && !String.valueOf(item).isBlank()) {
                values.add(String.valueOf(item));
            }
        }
        return values;
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(LLM_TRIGGER_INVALID, "LLM 触发请求 JSON 格式不合法");
        }
    }

    private String resolveTriggerPromptVersion(AiReviewConfig config) {
        String promptVersion = config.getPromptVersion();
        if (promptVersion == null || promptVersion.isBlank()) {
            return "llm-trigger:v1";
        }
        return promptVersion.trim();
    }

    private record TemplateContext(Long componentId,
                                   List<PromptTemplateEngine.SchemaField> schemaFields,
                                   List<String> targetFields) {
    }

    private record TriggerPrompt(List<LlmMessage> messages, PromptMode promptMode) {
    }
}
