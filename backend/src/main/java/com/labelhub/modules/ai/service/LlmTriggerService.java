package com.labelhub.modules.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final LlmTriggerRunMapper llmTriggerRunMapper;
    private final TemplateVersionMapper templateVersionMapper;
    private final AiReviewConfigMapper aiReviewConfigMapper;
    private final ObjectMapper objectMapper;
    private final boolean immediateExecution;
    @Autowired(required = false)
    private MediaContextResolver mediaContextResolver;
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
                             LlmTriggerRunMapper llmTriggerRunMapper,
                             TemplateVersionMapper templateVersionMapper,
                             AiReviewConfigMapper aiReviewConfigMapper) {
        this(taskMapper, datasetItemMapper, assignmentMapper, llmProviderService, rateLimiter,
                llmGateway, agentRunService, auditAppender, traceIdProvider, llmTaskQueueService,
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
                null, null, new ObjectMapper(), true);
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
                llmTriggerRunMapper, templateVersionMapper, aiReviewConfigMapper, objectMapper, false);
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
        this.llmTriggerRunMapper = llmTriggerRunMapper;
        this.templateVersionMapper = templateVersionMapper;
        this.aiReviewConfigMapper = aiReviewConfigMapper;
        this.objectMapper = objectMapper;
        this.immediateExecution = immediateExecution;
    }

    // ── Labeler 从 assignment 触发 ──

    public LlmTriggerRunResponse runForAssignment(CurrentUser currentUser, Long assignmentId,
                                                   LlmTriggerRunRequest request) {
        if (!currentUser.roles().contains(RoleCode.LABELER)) {
            throw new BusinessException(FORBIDDEN, "Only labelers can trigger from assignment");
        }
        Assignment assignment = assignmentMapper.selectOwnedAssignment(assignmentId, currentUser.userId());
        if (assignment == null) {
            throw new BusinessException(ASSIGNMENT_NOT_FOUND, "Assignment not found");
        }
        Task task = loadTask(assignment.getTaskId());
        DatasetItem datasetItem = loadDatasetItem(assignment.getDatasetItemId(), task.getId());
        TemplateVersion templateVersion = loadTemplateVersion(assignment.getTemplateVersionId());
        AiReviewConfig config = loadTaskAiReviewConfig(task);
        ComponentContext component = resolveComponent(templateVersion.getSchemaJson(), request.componentId());
        requireEnabledProvider(config.getProviderId());

        return doRun(task, assignment.getTemplateVersionId(), datasetItem, config, component,
                request, currentUser.userId(), assignmentId);
    }

    // ── Owner 预览测试 ──

    public LlmTriggerRunResponse testFromTask(CurrentUser currentUser, Long taskId,
                                               LlmTriggerRunRequest request) {
        if (!currentUser.roles().contains(RoleCode.OWNER)) {
            throw new BusinessException(FORBIDDEN, "Only owners can test LlmTrigger");
        }
        Task task = loadTask(taskId);
        if (!currentUser.userId().equals(task.getOwnerId())) {
            throw new BusinessException(FORBIDDEN, "Not the task owner");
        }
        DatasetItem datasetItem = loadDatasetItem(request.datasetItemId(), task.getId());
        TemplateVersion templateVersion = loadTemplateVersion(task.getPublishedTemplateVersionId());
        AiReviewConfig config = loadTaskAiReviewConfig(task);
        ComponentContext component = resolveComponent(templateVersion.getSchemaJson(), request.componentId());
        requireEnabledProvider(config.getProviderId());

        return doRun(task, task.getPublishedTemplateVersionId(), datasetItem, config, component,
                request, currentUser.userId(), null);
    }

    // ── 公共执行逻辑 ──

    private LlmTriggerRunResponse doRun(Task task, Long templateVersionId,
                                         DatasetItem datasetItem, AiReviewConfig config,
                                         ComponentContext component, LlmTriggerRunRequest request,
                                         Long actorId, Long assignmentId) {
        String traceId = traceIdProvider.currentTraceId();
        Map<String, Object> inputSnapshot = buildInputSnapshot(task, datasetItem, config, component, request, traceId);
        AgentRun agentRun = agentRunService.create(AGENT_TYPE, null, config.getProviderId(),
                config.getModelName().trim(),
                "target:" + String.join(",", component.targetFields()),
                toJson(inputSnapshot), assignmentId, traceId);
        agentRunService.start(agentRun.getId());

        LlmTriggerRun run = new LlmTriggerRun();
        run.setTaskId(task.getId());
        run.setAssignmentId(assignmentId);
        run.setTemplateVersionId(templateVersionId);
        run.setDatasetItemId(datasetItem != null ? datasetItem.getId() : request.datasetItemId());
        run.setComponentId(component.componentId());
        run.setProviderId(config.getProviderId());
        run.setModelName(config.getModelName().trim());
        run.setAgentRunId(agentRun.getId());
        run.setStatus(LlmTaskStatus.RUNNING.name());
        run.setTargetFieldsJson(toJson(component.targetFields()));
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
            llmTaskQueueService.enqueue(new LlmTaskQueueMessage(
                    LlmTaskType.LLM_TRIGGER,
                    run.getId(),
                    task.getId(),
                    assignmentId,
                    null,
                    null,
                    run.getId(),
                    agentRun.getId(),
                    traceId,
                    0,
                    Instant.now()
            ));
        }

        appendAudit(actorId, task.getId(), request, run);
        return toRunResponse(run);
    }

    public LlmTriggerRunResponse getRun(CurrentUser currentUser, Long triggerRunId) {
        LlmTriggerRun run = llmTriggerRunMapper.selectById(triggerRunId);
        if (run == null) {
            throw new BusinessException(RUN_NOT_FOUND, "LLM trigger run not found");
        }
        Task task = loadTask(run.getTaskId());
        if (currentUser.roles().contains(RoleCode.OWNER) && currentUser.userId().equals(task.getOwnerId())) {
            return toRunResponse(run);
        }
        if (run.getAssignmentId() != null && currentUser.roles().contains(RoleCode.LABELER)) {
            Assignment assignment = assignmentMapper.selectOwnedAssignment(run.getAssignmentId(), currentUser.userId());
            if (assignment != null) {
                return toRunResponse(run);
            }
        }
        throw new BusinessException(FORBIDDEN, "No permission to view LlmTrigger run");
    }

    public void executeQueuedTrigger(Long triggerRunId) {
        LlmTriggerRun run = llmTriggerRunMapper.selectById(triggerRunId);
        if (run == null || LlmTaskStatus.SUCCESS.name().equals(run.getStatus())) {
            return;
        }
        Task task = loadTask(run.getTaskId());
        executeRun(run, task, run.getProviderId(), run.getModelName());
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
        String systemPrompt = promptTemplateEngine.buildLlmTriggerPrompt(userTemplate, ctx,
                run.getComponentId(), parseStringList(run.getTargetFieldsJson()),
                run.getInputSnapshotJson());

        LlmGatewayResponse gatewayResponse = llmGateway.review(new LlmGatewayRequest(
                providerId,
                modelName,
                List.of(
                        new LlmMessage("system", systemPrompt),
                        new LlmMessage("user", run.getInputSnapshotJson())
                )
        ));
        if (gatewayResponse.status() == LlmGatewayStatus.SUCCESS) {
            Map<String, Object> normalizedResult = normalizeStructuredPatch(
                    gatewayResponse.structuredJson(), run.getComponentId(), parseStringList(run.getTargetFieldsJson()));
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

    private Task loadTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(TASK_NOT_FOUND, "Task not found");
        }
        return task;
    }

    private void requireEnabledProvider(Long providerId) {
        if (llmProviderService.findEnabledById(providerId).isEmpty()) {
            throw new BusinessException(LLM_TRIGGER_PROVIDER_UNAVAILABLE, "Enabled LLM provider is required");
        }
    }

    private DatasetItem loadDatasetItem(Long datasetItemId, Long taskId) {
        if (datasetItemId == null) {
            return null;
        }
        DatasetItem datasetItem = datasetItemMapper.selectById(datasetItemId);
        if (datasetItem == null || !taskId.equals(datasetItem.getTaskId())) {
            throw new BusinessException(DATASET_ITEM_NOT_FOUND, "Dataset item not found");
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

    private ComponentContext resolveComponent(String schemaJson, String componentId) {
        if (componentId == null || componentId.isBlank()) {
            throw new BusinessException(LLM_TRIGGER_INVALID, "componentId is required");
        }
        Object schema = parseJsonValue(schemaJson);
        Map<String, Object> component = findComponent(schema, componentId.trim());
        if (component == null) {
            throw new BusinessException(LLM_TRIGGER_INVALID, "LlmTrigger component not found");
        }
        return new ComponentContext(componentId.trim(), component, resolveTargetFields(component, componentId.trim()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findComponent(Object node, String componentId) {
        if (node instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = (Map<String, Object>) rawMap;
            if (matchesComponent(map, componentId)) {
                return map;
            }
            for (Object value : map.values()) {
                Map<String, Object> found = findComponent(value, componentId);
                if (found != null) {
                    return found;
                }
            }
        } else if (node instanceof List<?> list) {
            for (Object value : list) {
                Map<String, Object> found = findComponent(value, componentId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private boolean matchesComponent(Map<String, Object> component, String componentId) {
        return componentId.equals(stringValue(component.get("id"), null))
                || componentId.equals(stringValue(component.get("componentId"), null))
                || componentId.equals(stringValue(component.get("field"), null))
                || componentId.equals(stringValue(component.get("name"), null));
    }

    private List<String> resolveTargetFields(Map<String, Object> component, String componentId) {
        Object targetFields = component.get("targetFields");
        if (targetFields instanceof List<?> list) {
            List<String> fields = list.stream()
                    .map(value -> stringValue(value, null))
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .toList();
            if (!fields.isEmpty()) {
                return fields;
            }
        }
        String field = stringValue(component.get("field"), null);
        if (field != null && !field.isBlank()) {
            return List.of(field);
        }
        return List.of(componentId);
    }

    private Map<String, Object> buildInputSnapshot(Task task, DatasetItem datasetItem, AiReviewConfig config,
                                                   ComponentContext component, LlmTriggerRunRequest request,
                                                   String traceId) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("traceId", traceId);
        input.put("task", Map.of(
                "taskId", task.getId(),
                "title", task.getTitle() == null ? "" : task.getTitle(),
                "description", task.getDescription() == null ? "" : task.getDescription(),
                "instructionRichText", task.getInstructionRichText() == null ? "" : task.getInstructionRichText()
        ));
        input.put("componentId", component.componentId());
        input.put("component", component.component());
        input.put("targetFields", component.targetFields());
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
        return "Use the task, itemSnapshot, component, currentAnswerJson and scoringDimensions to produce a JSON "
                + "object that can be merged into the answer. Return exactly: componentId, targetFields, patch, "
                + "displayText, confidence, reasoningSummary, warnings. Only include targetFields in patch.";
    }

    public LlmTriggerRunResponse toRunResponse(LlmTriggerRun run) {
        Map<String, Object> result = parseObjectMapOrEmpty(run.getResultJson());
        Map<String, Object> input = parseObjectMapOrEmpty(run.getInputSnapshotJson());
        return new LlmTriggerRunResponse(
                run.getId(),
                run.getAgentRunId(),
                run.getComponentId(),
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
                                                         String componentId,
                                                         List<String> targetFields) {
        Map<String, Object> source = structuredJson == null ? Map.of() : structuredJson;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("componentId", stringValue(source.get("componentId"), componentId));
        result.put("targetFields", targetFields);
        List<String> warnings = new ArrayList<>(parseWarnings(source.get("warnings")));
        Map<String, Object> sourcePatch;
        if (source.get("patch") instanceof Map<?, ?> map) {
            sourcePatch = castObjectMap(map);
        } else {
            log.warn("LLM trigger response missing 'patch' field for component {}; "
                    + "using entire response as patch source", componentId);
            sourcePatch = source;
            warnings.add("LLM response missing 'patch' wrapper; results may be incomplete");
        }
        Map<String, Object> patch = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : sourcePatch.entrySet()) {
            if (targetFields.contains(entry.getKey())) {
                patch.put(entry.getKey(), entry.getValue());
            } else if (!List.of("componentId", "targetFields", "displayText", "confidence",
                    "reasoningSummary", "warnings", "patch").contains(entry.getKey())) {
                warnings.add("Dropped non-target patch field: " + entry.getKey());
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

    private Object parseJsonValue(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(LLM_TRIGGER_INVALID, "Dataset item JSON is invalid");
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(LLM_TRIGGER_INVALID, "LlmTrigger payload JSON is invalid");
        }
    }

    private record ComponentContext(String componentId,
                                    Map<String, Object> component,
                                    List<String> targetFields) {
    }
}
