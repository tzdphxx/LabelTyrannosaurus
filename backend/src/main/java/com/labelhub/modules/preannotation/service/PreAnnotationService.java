package com.labelhub.modules.preannotation.service;

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
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.service.AgentRunService;
import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.domain.LlmProvider;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.ai.service.MediaPromptContextBuilder;
import com.labelhub.modules.ai.service.MediaPromptInput;
import com.labelhub.modules.ai.service.MediaPromptResult;
import com.labelhub.modules.ai.service.ProviderCapability;
import com.labelhub.modules.ai.service.LlmProviderService;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.preannotation.domain.PreAnnotation;
import com.labelhub.modules.preannotation.domain.PreAnnotationStatus;
import com.labelhub.modules.preannotation.dto.PreAnnotationResponse;
import com.labelhub.modules.preannotation.mapper.PreAnnotationMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreAnnotationService {

    private static final int FORBIDDEN = 403801;
    private static final int NOT_FOUND = 404801;
    private static final int CONFIG_NOT_FOUND = 404802;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                                MediaPromptContextBuilder mediaPromptContextBuilder) {
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
    }

    @Transactional
    public PreAnnotationResponse run(Long assignmentId, Long labelerId) {
        Assignment assignment = assignmentMapper.selectOwnedAssignment(assignmentId, labelerId);
        if (assignment == null) {
            throw new BusinessException(FORBIDDEN, "Forbidden");
        }
        Task task = taskMapper.selectById(assignment.getTaskId());
        AiReviewConfig config = loadConfig(task);
        DatasetItem item = datasetItemMapper.selectById(assignment.getDatasetItemId());
        LlmProvider provider = loadProvider(config.getProviderId());
        ProviderCapability capability = llmProviderService.capability(provider);
        MediaPromptResult prompt = mediaPromptContextBuilder.build(new MediaPromptInput(
                item == null ? null : item.getItemJson(),
                assignment.getDraftAnswerJson(),
                preAnnotationPrompt(config),
                capability,
                config.getMultimodalEnabled() == null || Boolean.TRUE.equals(config.getMultimodalEnabled()),
                config.getVisionDetail() != null ? config.getVisionDetail() : "auto",
                config.getMaxImagesPerRequest() != null ? config.getMaxImagesPerRequest() : 5
        ));
        AgentRun run = agentRunService.create(AGENT_TYPE, null, config.getProviderId(), config.getModelName(),
                config.getPromptVersion(), prompt.promptSnapshot(), assignment.getId());
        agentRunService.start(run.getId());

        LlmGatewayResponse gatewayResponse = llmGateway.review(new LlmGatewayRequest(
                config.getProviderId(),
                config.getModelName(),
                withSystemPrompt(prompt.messages())
        ));
        PreAnnotation record = baseRecord(assignment, run.getId(), prompt);
        if (gatewayResponse.status() == LlmGatewayStatus.SUCCESS) {
            agentRunService.complete(run.getId(), toJson(gatewayResponseSnapshot(gatewayResponse)));
            fillSuccess(record, gatewayResponse, prompt);
        } else {
            AgentRunStatus runStatus = gatewayResponse.status() == LlmGatewayStatus.RATE_LIMITED
                    ? AgentRunStatus.RATE_LIMITED : AgentRunStatus.FAILED;
            agentRunService.fail(run.getId(), runStatus, gatewayResponse.errorMessage());
            record.setStatus(gatewayResponse.status() == LlmGatewayStatus.RATE_LIMITED
                    ? PreAnnotationStatus.RATE_LIMITED : PreAnnotationStatus.FAILED);
            record.setErrorCode(gatewayResponse.errorCode());
            record.setErrorMessage(gatewayResponse.errorMessage());
            record.setRawResponse(gatewayResponse.rawResponse());
        }
        preAnnotationMapper.insert(record);
        appendAudit(labelerId, task, record);
        return toResponse(record, false);
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

    private String preAnnotationPrompt(AiReviewConfig config) {
        return "You are LabelHub pre-annotation assistant. Return valid JSON only with keys: "
                + "suggestedAnswerJson, fieldSuggestions, riskFlags, overallConfidence, limitations. "
                + "Task guidance: " + config.getPromptTemplate();
    }

    private List<LlmMessage> withSystemPrompt(List<LlmMessage> messages) {
        return java.util.stream.Stream.concat(
                java.util.stream.Stream.of(new LlmMessage("system", "Return valid JSON only.")),
                messages.stream()).toList();
    }

    private PreAnnotation baseRecord(Assignment assignment, Long agentRunId, MediaPromptResult prompt) {
        PreAnnotation record = new PreAnnotation();
        record.setAssignmentId(assignment.getId());
        record.setTaskId(assignment.getTaskId());
        record.setDatasetItemId(assignment.getDatasetItemId());
        record.setLabelerId(assignment.getLabelerId());
        record.setAgentRunId(agentRunId);
        record.setPromptMode(prompt.promptMode().name());
        record.setDegraded(prompt.degraded());
        record.setLimitations(toJson(prompt.limitations()));
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        return record;
    }

    private void fillSuccess(PreAnnotation record, LlmGatewayResponse response, MediaPromptResult prompt) {
        Map<String, Object> json = response.structuredJson() != null ? response.structuredJson() : Map.of();
        record.setStatus(PreAnnotationStatus.SUCCESS);
        record.setSuggestedAnswerJson(toJson(json.getOrDefault("suggestedAnswerJson", Map.of())));
        record.setFieldSuggestions(toJson(json.getOrDefault("fieldSuggestions", List.of())));
        record.setRiskFlags(toJson(json.getOrDefault("riskFlags", List.of())));
        record.setOverallConfidence(asBigDecimal(json.get("overallConfidence")));
        record.setLimitations(toJson(mergeLimitations(prompt.limitations(), json.get("limitations"))));
        record.setRawResponse(response.rawResponse());
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
        auditAppender.append(new AuditCommand("USER", actorId, "PRE_ANNOTATION",
                record.getId() != null ? record.getId() : record.getAssignmentId(),
                "PRE_ANNOTATION_RUN", null, snapshot, traceIdProvider.currentTraceId(), record.getAgentRunId()));
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
        return new BigDecimal(String.valueOf(value));
    }
}
