package com.labelhub.modules.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import com.labelhub.infrastructure.llmtask.LlmTaskQueueMessage;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueService;
import com.labelhub.infrastructure.llmtask.LlmTaskStatus;
import com.labelhub.infrastructure.llmtask.LlmTaskType;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.service.AgentRunService;
import com.labelhub.modules.ai.domain.LlmTriggerRun;
import com.labelhub.modules.ai.dto.LlmTriggerRunRequest;
import com.labelhub.modules.ai.dto.LlmTriggerRunResponse;
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

    private static final int FORBIDDEN = 403001;
    private static final int TASK_NOT_FOUND = 404001;
    private static final int LLM_TRIGGER_INVALID = 400501;
    private static final int TEMPLATE_VERSION_NOT_FOUND = 404501;
    private static final int DATASET_ITEM_NOT_FOUND = 404502;
    private static final int LLM_TRIGGER_PROVIDER_UNAVAILABLE = 400503;
    private static final int RUN_NOT_FOUND = 404503;
    private static final String BIZ_TYPE = "LLM_TRIGGER";
    private static final String USER_ACTOR_TYPE = "USER";
    private static final String AGENT_TYPE = "LLM_TRIGGER";
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final TaskMapper taskMapper;
    private final TemplateVersionMapper templateVersionMapper;
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
    private final ObjectMapper objectMapper;
    private final boolean immediateExecution;
    @Autowired(required = false)
    private MediaContextResolver mediaContextResolver;

    @Autowired
    public LlmTriggerService(TaskMapper taskMapper,
                             TemplateVersionMapper templateVersionMapper,
                             DatasetItemMapper datasetItemMapper,
                             AssignmentMapper assignmentMapper,
                             LlmProviderService llmProviderService,
                             LlmTriggerRateLimiter rateLimiter,
                             LlmGateway llmGateway,
                             AgentRunService agentRunService,
                             AuditAppender auditAppender,
                             TraceIdProvider traceIdProvider,
                             LlmTaskQueueService llmTaskQueueService,
                             LlmTriggerRunMapper llmTriggerRunMapper) {
        this(taskMapper, templateVersionMapper, datasetItemMapper, assignmentMapper, llmProviderService, rateLimiter,
                llmGateway, agentRunService, auditAppender, traceIdProvider, llmTaskQueueService,
                llmTriggerRunMapper, new ObjectMapper(), false);
    }

    public LlmTriggerService(TaskMapper taskMapper,
                             TemplateVersionMapper templateVersionMapper,
                             DatasetItemMapper datasetItemMapper,
                             AssignmentMapper assignmentMapper,
                             LlmProviderService llmProviderService,
                             LlmTriggerRateLimiter rateLimiter,
                             LlmGateway llmGateway,
                             AgentRunService agentRunService,
                             AuditAppender auditAppender,
                             TraceIdProvider traceIdProvider) {
        this(taskMapper, templateVersionMapper, datasetItemMapper, assignmentMapper, llmProviderService, rateLimiter,
                llmGateway, agentRunService, auditAppender, traceIdProvider, null, null,
                new ObjectMapper(), true);
    }

    LlmTriggerService(TaskMapper taskMapper,
                      TemplateVersionMapper templateVersionMapper,
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
                      ObjectMapper objectMapper) {
        this(taskMapper, templateVersionMapper, datasetItemMapper, assignmentMapper, llmProviderService, rateLimiter,
                llmGateway, agentRunService, auditAppender, traceIdProvider, llmTaskQueueService,
                llmTriggerRunMapper, objectMapper, false);
    }

    private LlmTriggerService(TaskMapper taskMapper,
                              TemplateVersionMapper templateVersionMapper,
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
                              ObjectMapper objectMapper,
                              boolean immediateExecution) {
        this.taskMapper = taskMapper;
        this.templateVersionMapper = templateVersionMapper;
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
        this.objectMapper = objectMapper;
        this.immediateExecution = immediateExecution;
    }

    public LlmTriggerRunResponse run(CurrentUser currentUser, LlmTriggerRunRequest request) {
        Task task = loadTask(request.taskId());
        TemplateVersion templateVersion = loadTemplateVersion(request.templateVersionId(), task.getId());
        validateAccess(currentUser, task, templateVersion, request);
        DatasetItem datasetItem = loadDatasetItem(request.datasetItemId(), task.getId());
        LlmTriggerComponent component = loadComponent(templateVersion, request.componentId());
        requireEnabledProvider(task.getOwnerId(), component.providerId());
        Map<String, Object> inputSnapshot = inputSnapshot(component, datasetItem, request);

        AgentRun agentRun = agentRunService.create(AGENT_TYPE, null, component.providerId(), component.modelName(),
                "component:" + component.componentId(), toJson(inputSnapshot), request.assignmentId());
        agentRunService.start(agentRun.getId());

        LlmTriggerRun run = new LlmTriggerRun();
        run.setTaskId(task.getId());
        run.setAssignmentId(request.assignmentId());
        run.setTemplateVersionId(templateVersion.getId());
        run.setDatasetItemId(request.datasetItemId());
        run.setComponentId(component.componentId());
        run.setProviderId(component.providerId());
        run.setModelName(component.modelName());
        run.setAgentRunId(agentRun.getId());
        run.setStatus(LlmTaskStatus.RUNNING.name());
        run.setTargetFieldsJson(toJson(component.targetFields()));
        run.setInputSnapshotJson(toJson(inputSnapshot));
        run.setCreatedBy(currentUser.userId());
        run.setCreatedAt(LocalDateTime.now());
        run.setUpdatedAt(LocalDateTime.now());
        if (llmTriggerRunMapper != null) {
            llmTriggerRunMapper.insert(run);
        } else {
            run.setId(agentRun.getId());
        }

        if (immediateExecution) {
            executeRun(run, task, component);
        } else {
            llmTaskQueueService.enqueue(new LlmTaskQueueMessage(
                    LlmTaskType.LLM_TRIGGER,
                    run.getId(),
                    task.getId(),
                    request.assignmentId(),
                    null,
                    null,
                    run.getId(),
                    agentRun.getId(),
                    traceIdProvider.currentTraceId(),
                    0,
                    Instant.now()
            ));
        }

        appendAudit(currentUser.userId(), task.getId(), request, component, run);
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
        LlmTriggerComponent component = new LlmTriggerComponent(
                run.getComponentId(),
                run.getProviderId(),
                run.getModelName(),
                null,
                parseStringList(run.getTargetFieldsJson())
        );
        executeRun(run, task, component);
    }

    private void executeRun(LlmTriggerRun run, Task task, LlmTriggerComponent component) {
        if (!rateLimiter.acquire(run.getTaskId(), run.getCreatedBy(), run.getProviderId())) {
            agentRunService.fail(run.getAgentRunId(), AgentRunStatus.RATE_LIMITED, "LLM trigger rate limited");
            fillFailure(run, LlmTaskStatus.RATE_LIMITED.name(), "RATE_LIMITED", "LLM trigger rate limited");
            appendAudit(run.getCreatedBy(), task.getId(), null, component, run);
            return;
        }

        LlmGatewayResponse gatewayResponse = llmGateway.review(new LlmGatewayRequest(
                run.getProviderId(),
                run.getModelName(),
                List.of(
                        new LlmMessage("system", "You are a LabelHub field-level LlmTrigger assistant. Return JSON."),
                        new LlmMessage("user", run.getInputSnapshotJson())
                )
        ));
        if (gatewayResponse.status() == LlmGatewayStatus.SUCCESS) {
            agentRunService.complete(run.getAgentRunId(), toJson(gatewayResponseSnapshot(gatewayResponse)));
            run.setStatus(LlmTaskStatus.SUCCESS.name());
            run.setResultJson(toJson(gatewayResponse.structuredJson()));
            run.setContentText(gatewayResponse.contentText());
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
        }
        run.setUpdatedAt(LocalDateTime.now());
        if (llmTriggerRunMapper != null) {
            llmTriggerRunMapper.updateById(run);
        }
        appendAudit(run.getCreatedBy(), task.getId(), null, component, run);
    }

    private Task loadTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(TASK_NOT_FOUND, "Task not found");
        }
        return task;
    }

    private TemplateVersion loadTemplateVersion(Long templateVersionId, Long taskId) {
        TemplateVersion templateVersion = templateVersionMapper.selectById(templateVersionId);
        if (templateVersion == null || !taskId.equals(templateVersion.getTaskId())) {
            throw new BusinessException(TEMPLATE_VERSION_NOT_FOUND, "Template version not found");
        }
        return templateVersion;
    }

    private void validateAccess(CurrentUser currentUser, Task task, TemplateVersion templateVersion,
                                LlmTriggerRunRequest request) {
        if (request.previewMode()) {
            if (!currentUser.roles().contains(RoleCode.OWNER) || !currentUser.userId().equals(task.getOwnerId())) {
                throw new BusinessException(FORBIDDEN, "No permission to preview LlmTrigger");
            }
            return;
        }
        if (!currentUser.roles().contains(RoleCode.LABELER) || request.assignmentId() == null) {
            throw new BusinessException(FORBIDDEN, "Labeler assignment is required to run LlmTrigger");
        }
        Assignment assignment = assignmentMapper.selectOwnedAssignment(request.assignmentId(), currentUser.userId());
        if (assignment == null
                || !task.getId().equals(assignment.getTaskId())
                || !templateVersion.getId().equals(assignment.getTemplateVersionId())
                || (request.datasetItemId() != null && !request.datasetItemId().equals(assignment.getDatasetItemId()))) {
            throw new BusinessException(FORBIDDEN, "No permission to run LlmTrigger");
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

    private LlmTriggerComponent loadComponent(TemplateVersion templateVersion, String componentId) {
        Map<String, Object> schema = parseObjectMap(templateVersion.getSchemaJson());
        Map<String, Object> component = findComponent(schema, componentId);
        if (component == null || !"LlmTrigger".equals(component.get("type"))) {
            throw new BusinessException(LLM_TRIGGER_INVALID, "Component must be LlmTrigger");
        }
        return toComponent(componentId, component);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findComponent(Object node, String componentId) {
        if (node instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = (Map<String, Object>) rawMap;
            Object id = map.get("id") != null ? map.get("id") : map.get("componentId");
            if (componentId.equals(id)) {
                return map;
            }
            for (Object value : map.values()) {
                Map<String, Object> found = findComponent(value, componentId);
                if (found != null) {
                    return found;
                }
            }
        } else if (node instanceof Iterable<?> values) {
            for (Object value : values) {
                Map<String, Object> found = findComponent(value, componentId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private LlmTriggerComponent toComponent(String requestedComponentId, Map<String, Object> component) {
        Long providerId = asLong(component.get("providerId"));
        String modelName = asText(component.get("modelName"));
        String promptTemplate = asText(component.get("promptTemplate"));
        if (providerId == null || isBlank(modelName) || isBlank(promptTemplate)) {
            throw new BusinessException(LLM_TRIGGER_INVALID,
                    "LlmTrigger providerId, modelName and promptTemplate are required");
        }
        return new LlmTriggerComponent(
                requestedComponentId,
                providerId,
                modelName.trim(),
                promptTemplate.trim(),
                asStringList(component.get("targetFields"))
        );
    }

    private void requireEnabledProvider(Long ownerId, Long providerId) {
        if (llmProviderService.findEnabledById(providerId).isEmpty()) {
            throw new BusinessException(LLM_TRIGGER_PROVIDER_UNAVAILABLE, "Enabled LLM provider is required");
        }
    }

    private Map<String, Object> inputSnapshot(LlmTriggerComponent component, DatasetItem datasetItem,
                                              LlmTriggerRunRequest request) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("componentId", component.componentId());
        input.put("promptTemplate", component.promptTemplate());
        input.put("targetFields", component.targetFields());
        input.put("previewMode", request.previewMode());
        String itemJson = datasetItem == null ? null : datasetItem.getItemJson();
        if (mediaContextResolver != null && datasetItem != null) {
            itemJson = mediaContextResolver.resolveItemJson(datasetItem.getId(), itemJson);
        }
        input.put("itemSnapshot", datasetItem == null ? Map.of() : parseJsonValue(itemJson));
        input.put("currentAnswerJson", request.currentAnswerJson() == null ? Map.of() : request.currentAnswerJson());
        return input;
    }

    public LlmTriggerRunResponse toRunResponse(LlmTriggerRun run) {
        return new LlmTriggerRunResponse(
                run.getId(),
                run.getAgentRunId(),
                run.getComponentId(),
                parseObjectMapOrEmpty(run.getResultJson()),
                run.getContentText(),
                parseStringList(run.getTargetFieldsJson()),
                run.getContentText(),
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

    private void appendAudit(Long actorId, Long taskId, LlmTriggerRunRequest request,
                             LlmTriggerComponent component, LlmTriggerRun run) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("taskId", taskId);
        snapshot.put("triggerRunId", run.getId());
        snapshot.put("componentId", component.componentId());
        snapshot.put("providerId", component.providerId());
        snapshot.put("modelName", component.modelName());
        snapshot.put("targetFields", component.targetFields());
        snapshot.put("previewMode", request != null && request.previewMode());
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

    private Object parseJsonValue(String json) {
        if (isBlank(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(LLM_TRIGGER_INVALID, "Dataset item JSON is invalid");
        }
    }

    private Map<String, Object> parseObjectMap(String json) {
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(LLM_TRIGGER_INVALID, "Template schema JSON is invalid");
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

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private List<String> asStringList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (Object item : iterable) {
            if (item != null && !String.valueOf(item).isBlank()) {
                values.add(String.valueOf(item).trim());
            }
        }
        return values;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record LlmTriggerComponent(
            String componentId,
            Long providerId,
            String modelName,
            String promptTemplate,
            List<String> targetFields
    ) {
    }
}
