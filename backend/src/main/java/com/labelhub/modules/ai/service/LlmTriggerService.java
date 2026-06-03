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
import java.time.Instant;
import java.time.LocalDateTime;
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
    private static final int DATASET_ITEM_NOT_FOUND = 404502;
    private static final int LLM_TRIGGER_PROVIDER_UNAVAILABLE = 400503;
    private static final int RUN_NOT_FOUND = 404503;
    private static final int ASSIGNMENT_NOT_FOUND = 404504;
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
    private final ObjectMapper objectMapper;
    private final boolean immediateExecution;
    @Autowired(required = false)
    private MediaContextResolver mediaContextResolver;

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
                             LlmTriggerRunMapper llmTriggerRunMapper) {
        this(taskMapper, datasetItemMapper, assignmentMapper, llmProviderService, rateLimiter,
                llmGateway, agentRunService, auditAppender, traceIdProvider, llmTaskQueueService,
                llmTriggerRunMapper, new ObjectMapper(), false);
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
                new ObjectMapper(), true);
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
                      ObjectMapper objectMapper) {
        this(taskMapper, datasetItemMapper, assignmentMapper, llmProviderService, rateLimiter,
                llmGateway, agentRunService, auditAppender, traceIdProvider, llmTaskQueueService,
                llmTriggerRunMapper, objectMapper, false);
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
        requireEnabledProvider(request.providerId());
        DatasetItem datasetItem = loadDatasetItem(assignment.getDatasetItemId(), task.getId());

        return doRun(task, assignment.getTemplateVersionId(), datasetItem,
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
        requireEnabledProvider(request.providerId());
        DatasetItem datasetItem = loadDatasetItem(request.datasetItemId(), task.getId());

        return doRun(task, task.getPublishedTemplateVersionId(), datasetItem,
                request, currentUser.userId(), null);
    }

    // ── 公共执行逻辑 ──

    private LlmTriggerRunResponse doRun(Task task, Long templateVersionId,
                                         DatasetItem datasetItem, LlmTriggerRunRequest request,
                                         Long actorId, Long assignmentId) {
        Map<String, Object> inputSnapshot = buildInputSnapshot(datasetItem, request);

        AgentRun agentRun = agentRunService.create(AGENT_TYPE, null, request.providerId(),
                request.modelName().trim(),
                "target:" + String.join(",", request.targetFields()),
                toJson(inputSnapshot), assignmentId);
        agentRunService.start(agentRun.getId());

        LlmTriggerRun run = new LlmTriggerRun();
        run.setTaskId(task.getId());
        run.setAssignmentId(assignmentId);
        run.setTemplateVersionId(templateVersionId);
        run.setDatasetItemId(datasetItem != null ? datasetItem.getId() : request.datasetItemId());
        run.setComponentId(null);
        run.setProviderId(request.providerId());
        run.setModelName(request.modelName().trim());
        run.setAgentRunId(agentRun.getId());
        run.setStatus(LlmTaskStatus.RUNNING.name());
        run.setTargetFieldsJson(toJson(request.targetFields()));
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
            executeRun(run, task, request.providerId(), request.modelName().trim());
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
                    traceIdProvider.currentTraceId(),
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

    private void executeRun(LlmTriggerRun run, Task task, Long providerId, String modelName) {
        if (!rateLimiter.acquire(run.getTaskId(), run.getCreatedBy(), providerId)) {
            agentRunService.fail(run.getAgentRunId(), AgentRunStatus.RATE_LIMITED, "LLM trigger rate limited");
            fillFailure(run, LlmTaskStatus.RATE_LIMITED.name(), "RATE_LIMITED", "LLM trigger rate limited");
            appendAudit(run.getCreatedBy(), task.getId(), null, run);
            return;
        }

        LlmGatewayResponse gatewayResponse = llmGateway.review(new LlmGatewayRequest(
                providerId,
                modelName,
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
        appendAudit(run.getCreatedBy(), task.getId(), null, run);
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

    private Map<String, Object> buildInputSnapshot(DatasetItem datasetItem, LlmTriggerRunRequest request) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("promptTemplate", request.promptTemplate().trim());
        input.put("targetFields", request.targetFields());
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
            throw new BusinessException(LLM_TRIGGER_INVALID, "LLM 触发请求 JSON 格式不合法");
        }
    }
}
