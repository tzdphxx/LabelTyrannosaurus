package com.labelhub.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.labelhub.modules.agent.service.AgentRunService;
import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.dto.AiReviewConfigRequest;
import com.labelhub.modules.ai.dto.AiReviewConfigResponse;
import com.labelhub.modules.ai.dto.AiReviewPromptTestRequest;
import com.labelhub.modules.ai.dto.AiReviewPromptTestResponse;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiReviewConfigService {

    private static final int TASK_NOT_FOUND = 404001;
    private static final int AI_REVIEW_PROVIDER_DISABLED = 400401;
    private static final int AI_REVIEW_CONFIG_INVALID = 400402;
    private static final int AI_REVIEW_CONFIG_NOT_FOUND = 404401;
    private static final String BIZ_TYPE = "AI_REVIEW_CONFIG";
    private static final String USER_ACTOR_TYPE = "USER";
    private static final String TEST_AGENT_TYPE = "AI_REVIEW_CONFIG_TEST";
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final AiReviewConfigMapper aiReviewConfigMapper;
    private final TaskMapper taskMapper;
    private final LlmProviderService llmProviderService;
    private final LlmGateway llmGateway;
    private final AgentRunService agentRunService;
    private final AuditAppender auditAppender;
    private final TraceIdProvider traceIdProvider;
    private final ObjectMapper objectMapper;

    public AiReviewConfigService(AiReviewConfigMapper aiReviewConfigMapper,
                                 TaskMapper taskMapper,
                                 LlmProviderService llmProviderService,
                                 LlmGateway llmGateway,
                                 AgentRunService agentRunService,
                                 AuditAppender auditAppender,
                                 TraceIdProvider traceIdProvider) {
        this(aiReviewConfigMapper, taskMapper, llmProviderService, llmGateway, agentRunService,
                auditAppender, traceIdProvider, new ObjectMapper());
    }

    AiReviewConfigService(AiReviewConfigMapper aiReviewConfigMapper,
                          TaskMapper taskMapper,
                          LlmProviderService llmProviderService,
                          LlmGateway llmGateway,
                          AgentRunService agentRunService,
                          AuditAppender auditAppender,
                          TraceIdProvider traceIdProvider,
                          ObjectMapper objectMapper) {
        this.aiReviewConfigMapper = aiReviewConfigMapper;
        this.taskMapper = taskMapper;
        this.llmProviderService = llmProviderService;
        this.llmGateway = llmGateway;
        this.agentRunService = agentRunService;
        this.auditAppender = auditAppender;
        this.traceIdProvider = traceIdProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiReviewConfigResponse save(Long ownerId, Long taskId, AiReviewConfigRequest request) {
        Task task = loadOwnedDraftTask(ownerId, taskId);
        validateRequest(request);
        requireEnabledProvider(request.providerId());
        AiReviewConfig existing = findByTaskId(taskId);
        if (existing != null) {
            return updateExisting(ownerId, task, existing, request, "AI_REVIEW_CONFIG_UPDATED");
        }

        AiReviewConfig config = new AiReviewConfig();
        applyRequest(config, taskId, request);
        config.setPromptVersion("v1");
        config.setCreatedBy(ownerId);
        aiReviewConfigMapper.insert(config);
        task.setAiReviewConfigId(config.getId());
        taskMapper.updateById(task);
        auditAppender.append(new AuditCommand(USER_ACTOR_TYPE, ownerId, BIZ_TYPE, config.getId(), "AI_REVIEW_CONFIG_CREATED",
                null, auditSnapshot(config), traceIdProvider.currentTraceId(), null));
        return toResponse(config);
    }

    @Transactional
    public AiReviewConfigResponse update(Long ownerId, Long taskId, Long configId, AiReviewConfigRequest request) {
        Task task = loadOwnedDraftTask(ownerId, taskId);
        AiReviewConfig config = loadTaskConfig(taskId, configId);
        validateRequest(request);
        requireEnabledProvider(request.providerId());
        return updateExisting(ownerId, task, config, request, "AI_REVIEW_CONFIG_UPDATED");
    }

    public AiReviewConfigResponse get(Long ownerId, Long taskId) {
        loadOwnedTask(ownerId, taskId);
        AiReviewConfig config = findByTaskId(taskId);
        if (config == null) {
            throw new BusinessException(AI_REVIEW_CONFIG_NOT_FOUND, "AI review config not found");
        }
        return toResponse(config);
    }

    public boolean existsForTask(Long taskId, Long configId) {
        if (taskId == null || configId == null) {
            return false;
        }
        AiReviewConfig config = aiReviewConfigMapper.selectById(configId);
        return config != null && taskId.equals(config.getTaskId());
    }

    @Transactional
    public AiReviewPromptTestResponse testPrompt(Long ownerId, Long taskId, Long configId,
                                                 AiReviewPromptTestRequest request) {
        loadOwnedTask(ownerId, taskId);
        AiReviewConfig config = loadTaskConfig(taskId, configId);
        String inputSnapshot = toJson(promptTestSnapshot(config, request));
        AgentRun run = agentRunService.create(TEST_AGENT_TYPE, null, config.getProviderId(), config.getModelName(),
                config.getPromptVersion(), inputSnapshot);
        agentRunService.start(run.getId());

        LlmGatewayResponse gatewayResponse = llmGateway.review(new LlmGatewayRequest(
                config.getProviderId(),
                config.getModelName(),
                List.of(
                        new LlmMessage("system", "You are LabelHub AI review config tester. Return valid JSON only."),
                        new LlmMessage("user", buildPrompt(config, request))
                )
        ));
        if (gatewayResponse.status() == LlmGatewayStatus.SUCCESS) {
            agentRunService.complete(run.getId(), toJson(gatewayResponseSnapshot(gatewayResponse)));
        } else {
            agentRunService.fail(run.getId(), AgentRunStatus.FAILED, gatewayResponse.errorMessage());
        }
        return new AiReviewPromptTestResponse(
                run.getId(),
                gatewayResponse.status(),
                gatewayResponse.contentText(),
                gatewayResponse.structuredJson(),
                gatewayResponse.rawResponse(),
                gatewayResponse.latencyMs(),
                gatewayResponse.errorCode(),
                gatewayResponse.errorMessage()
        );
    }

    private AiReviewConfigResponse updateExisting(Long ownerId, Task task, AiReviewConfig config,
                                                  AiReviewConfigRequest request, String action) {
        Map<String, Object> beforeJson = auditSnapshot(config);
        applyRequest(config, task.getId(), request);
        config.setPromptVersion(nextPromptVersion(config.getPromptVersion()));
        aiReviewConfigMapper.updateById(config);
        if (!config.getId().equals(task.getAiReviewConfigId())) {
            task.setAiReviewConfigId(config.getId());
            taskMapper.updateById(task);
        }
        auditAppender.append(new AuditCommand(USER_ACTOR_TYPE, ownerId, BIZ_TYPE, config.getId(), action,
                beforeJson, auditSnapshot(config), traceIdProvider.currentTraceId(), null));
        return toResponse(config);
    }

    private void applyRequest(AiReviewConfig config, Long taskId, AiReviewConfigRequest request) {
        config.setTaskId(taskId);
        config.setProviderId(request.providerId());
        config.setModelName(request.modelName().trim());
        config.setPromptTemplate(request.promptTemplate().trim());
        config.setScoringDimensionsJson(toJson(normalizeDimensions(request.scoringDimensions())));
        config.setPassThreshold(request.passThreshold());
        config.setManualReviewThreshold(request.manualReviewThreshold());
        config.setOutputSchemaJson(toJson(request.outputSchema()));
        config.setMaxRetry(request.maxRetry() != null ? request.maxRetry() : 3);
    }

    private void validateRequest(AiReviewConfigRequest request) {
        if (request.manualReviewThreshold().compareTo(request.passThreshold()) > 0) {
            throw new BusinessException(AI_REVIEW_CONFIG_INVALID,
                    "Manual review threshold must not be greater than pass threshold");
        }
        if (normalizeDimensions(request.scoringDimensions()).isEmpty()) {
            throw new BusinessException(AI_REVIEW_CONFIG_INVALID, "AI review scoring dimensions are required");
        }
        if (request.outputSchema() == null || request.outputSchema().isEmpty()) {
            throw new BusinessException(AI_REVIEW_CONFIG_INVALID, "AI review output schema is required");
        }
    }

    private void requireEnabledProvider(Long providerId) {
        if (llmProviderService.findEnabledById(providerId).isEmpty()) {
            throw new BusinessException(AI_REVIEW_PROVIDER_DISABLED, "Enabled LLM provider is required");
        }
    }

    private Task loadOwnedDraftTask(Long ownerId, Long taskId) {
        Task task = loadOwnedTask(ownerId, taskId);
        if (task.getStatus() != TaskStatus.DRAFT) {
            throw new BusinessException(AI_REVIEW_CONFIG_INVALID, "Only draft tasks can update AI review config");
        }
        return task;
    }

    private Task loadOwnedTask(Long ownerId, Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || !ownerId.equals(task.getOwnerId())) {
            throw new BusinessException(TASK_NOT_FOUND, "Task not found");
        }
        return task;
    }

    private AiReviewConfig loadTaskConfig(Long taskId, Long configId) {
        AiReviewConfig config = aiReviewConfigMapper.selectById(configId);
        if (config == null || !taskId.equals(config.getTaskId())) {
            throw new BusinessException(AI_REVIEW_CONFIG_NOT_FOUND, "AI review config not found");
        }
        return config;
    }

    private AiReviewConfig findByTaskId(Long taskId) {
        return aiReviewConfigMapper.selectOne(new QueryWrapper<AiReviewConfig>().eq("task_id", taskId));
    }

    private AiReviewConfigResponse toResponse(AiReviewConfig config) {
        return new AiReviewConfigResponse(
                config.getId(),
                config.getTaskId(),
                config.getProviderId(),
                config.getModelName(),
                config.getPromptTemplate(),
                parseDimensions(config.getScoringDimensionsJson()),
                config.getPassThreshold(),
                config.getManualReviewThreshold(),
                parseObjectMap(config.getOutputSchemaJson()),
                config.getPromptVersion(),
                config.getMaxRetry()
        );
    }

    private Map<String, Object> auditSnapshot(AiReviewConfig config) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", config.getId());
        snapshot.put("taskId", config.getTaskId());
        snapshot.put("providerId", config.getProviderId());
        snapshot.put("modelName", config.getModelName());
        snapshot.put("promptTemplate", config.getPromptTemplate());
        snapshot.put("scoringDimensions", parseDimensions(config.getScoringDimensionsJson()));
        snapshot.put("passThreshold", config.getPassThreshold());
        snapshot.put("manualReviewThreshold", config.getManualReviewThreshold());
        snapshot.put("outputSchema", parseObjectMap(config.getOutputSchemaJson()));
        snapshot.put("promptVersion", config.getPromptVersion());
        return snapshot;
    }

    private Map<String, Object> promptTestSnapshot(AiReviewConfig config, AiReviewPromptTestRequest request) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("config", auditSnapshot(config));
        snapshot.put("itemSnapshot", request.itemSnapshot());
        snapshot.put("answerJson", request.answerJson());
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

    private String buildPrompt(AiReviewConfig config, AiReviewPromptTestRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("promptTemplate", config.getPromptTemplate());
        payload.put("scoringDimensions", parseDimensions(config.getScoringDimensionsJson()));
        payload.put("passThreshold", config.getPassThreshold());
        payload.put("manualReviewThreshold", config.getManualReviewThreshold());
        payload.put("outputSchema", parseObjectMap(config.getOutputSchemaJson()));
        payload.put("itemSnapshot", request.itemSnapshot());
        payload.put("answerJson", request.answerJson());
        return toJson(payload);
    }

    private List<String> normalizeDimensions(List<String> dimensions) {
        if (dimensions == null) {
            return List.of();
        }
        return dimensions.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String nextPromptVersion(String currentVersion) {
        if (currentVersion == null || !currentVersion.startsWith("v")) {
            return "v1";
        }
        try {
            return "v" + (Integer.parseInt(currentVersion.substring(1)) + 1);
        } catch (NumberFormatException ex) {
            return "v1";
        }
    }

    private List<String> parseDimensions(String json) {
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(AI_REVIEW_CONFIG_INVALID, "AI review scoring dimensions are invalid");
        }
    }

    private Map<String, Object> parseObjectMap(String json) {
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(AI_REVIEW_CONFIG_INVALID, "AI review output schema is invalid");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(AI_REVIEW_CONFIG_INVALID, "AI review config JSON is invalid");
        }
    }
}
