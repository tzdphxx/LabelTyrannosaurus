package com.labelhub.modules.task.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.modules.ai.dto.AiReviewConfigResponse;
import com.labelhub.modules.ai.dto.LlmProviderResponse;
import com.labelhub.modules.ai.dto.AiReviewConfigRequest;
import com.labelhub.modules.ai.service.AiReviewConfigService;
import com.labelhub.modules.ai.service.LlmProviderService;
import com.labelhub.modules.dataset.dto.DatasetImportJobResponse;
import com.labelhub.modules.dataset.dto.DatasetImportRequest;
import com.labelhub.modules.dataset.service.DatasetImportService;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.domain.TaskTag;
import com.labelhub.modules.task.dto.CreateTaskResponse;
import com.labelhub.modules.task.dto.CreateTaskRequest;
import com.labelhub.modules.task.dto.OwnerTaskSummaryResponse;
import com.labelhub.modules.task.dto.TaskDetailResponse;
import com.labelhub.modules.task.dto.TaskLifecycleResponse;
import com.labelhub.modules.task.dto.UpdateTaskRequest;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.task.mapper.TaskTagMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskLifecycleService {

    private static final int TASK_NOT_FOUND = 404001;
    private static final int TASK_STATUS_NOT_ALLOWED = 400101;
    private static final int TASK_PUBLISH_REQUIREMENT_MISSING = 400102;
    private static final int SINGLE_LABELER_OVERLAP_COUNT = 1;
    private static final String TASK_BIZ_TYPE = "TASK";
    private static final String USER_ACTOR_TYPE = "USER";

    private final TaskMapper taskMapper;
    private final TaskTagMapper taskTagMapper;
    private final TaskPublishDependencyChecker publishDependencyChecker;
    private final AuditAppender auditAppender;
    private final TraceIdProvider traceIdProvider;
    private final DatasetImportService datasetImportService;
    private final AiReviewConfigService aiReviewConfigService;
    private final LlmProviderService llmProviderService;
    private final org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    public TaskLifecycleService(TaskMapper taskMapper,
                                TaskTagMapper taskTagMapper,
                                TaskPublishDependencyChecker publishDependencyChecker,
                                AuditAppender auditAppender,
                                TraceIdProvider traceIdProvider,
                                DatasetImportService datasetImportService,
                                AiReviewConfigService aiReviewConfigService,
                                LlmProviderService llmProviderService,
                                org.springframework.context.ApplicationEventPublisher applicationEventPublisher) {
        this.taskMapper = taskMapper;
        this.taskTagMapper = taskTagMapper;
        this.publishDependencyChecker = publishDependencyChecker;
        this.auditAppender = auditAppender;
        this.traceIdProvider = traceIdProvider;
        this.datasetImportService = datasetImportService;
        this.aiReviewConfigService = aiReviewConfigService;
        this.llmProviderService = llmProviderService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public List<OwnerTaskSummaryResponse> listOwnerTasks(Long ownerId) {
        return taskMapper.selectList(new QueryWrapper<Task>()
                        .eq("owner_id", ownerId)
                        .orderByDesc("updated_at")
                        .orderByDesc("id"))
                .stream()
                .map(task -> new OwnerTaskSummaryResponse(
                        task.getId(),
                        task.getTitle(),
                        task.getStatus(),
                        listTags(task.getId()),
                        task.getQuota(),
                        task.getClaimedCount(),
                        task.getOverlapCount(),
                        task.getDeadlineAt(),
                        task.getPublishedAt(),
                        task.getEndedAt(),
                        task.getCreatedAt(),
                        task.getUpdatedAt()
                ))
                .toList();
    }

    public TaskDetailResponse getOwnedTask(Long ownerId, Long taskId) {
        return toDetailResponse(loadOwnedTask(ownerId, taskId));
    }

    @Transactional
    public TaskLifecycleResponse create(Long ownerId, CreateTaskRequest request) {
        Task task = new Task();
        task.setOwnerId(ownerId);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setInstructionRichText(request.instructionRichText());
        task.setStatus(TaskStatus.DRAFT);
        task.setQuota(request.quota());
        task.setClaimedCount(0);
        task.setOverlapCount(SINGLE_LABELER_OVERLAP_COUNT);
        task.setDeadlineAt(request.deadlineAt());
        task.setPublishedTemplateVersionId(request.publishedTemplateVersionId());
        task.setAiReviewConfigId(request.aiReviewConfigId());
        task.setReviewLevelCount(request.reviewLevelCount() != null ? request.reviewLevelCount() : 1);
        task.setRewardVisible(true);
        taskMapper.insert(task);

        if (hasAiInlineConfig(request)) {
            AiReviewConfigRequest aiRequest = new AiReviewConfigRequest(
                    request.aiProviderId(),
                    request.aiModelName(),
                    request.aiPrompt(),
                    request.aiScoringDimensions(),
                    request.aiPassThreshold(),
                    request.aiManualReviewThreshold(),
                    null, null, null, null, null, null, null);
            var aiConfig = aiReviewConfigService.save(ownerId, task.getId(), aiRequest);
            task.setAiReviewConfigId(aiConfig.id());
            taskMapper.updateById(task);
        }

        replaceTags(task.getId(), request.tags());
        appendAudit(task, ownerId, "TASK_CREATED", null, snapshot(task));
        return new TaskLifecycleResponse(task.getId(), task.getStatus());
    }

    @Transactional
    public CreateTaskResponse createWithDataset(Long ownerId, CreateTaskRequest request) {
        TaskLifecycleResponse task = create(ownerId, request);
        DatasetImportJobResponse importJob = null;
        if (request.datasetFileId() != null) {
            importJob = datasetImportService.createAppendImport(
                    task.taskId(),
                    new DatasetImportRequest(request.datasetFileId())
            );
        }
        return new CreateTaskResponse(task.taskId(), task.status(), importJob);
    }

    @Transactional
    public TaskLifecycleResponse updateDraft(Long ownerId, Long taskId, UpdateTaskRequest request) {
        Task task = loadOwnedTask(ownerId, taskId);
        if (task.getStatus() != TaskStatus.DRAFT) {
            throw new BusinessException(TASK_STATUS_NOT_ALLOWED, "只有草稿状态的任务可以编辑");
        }
        Map<String, Object> beforeJson = snapshot(task);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setInstructionRichText(request.instructionRichText());
        task.setQuota(request.quota());
        task.setDeadlineAt(request.deadlineAt());
        task.setPublishedTemplateVersionId(request.publishedTemplateVersionId());
        task.setAiReviewConfigId(request.aiReviewConfigId());
        if (request.reviewLevelCount() != null) {
            task.setReviewLevelCount(request.reviewLevelCount());
        }
        taskMapper.updateById(task);
        taskTagMapper.delete(new QueryWrapper<TaskTag>().eq("task_id", taskId));
        replaceTags(taskId, request.tags());
        appendAudit(task, ownerId, "TASK_UPDATED", beforeJson, snapshot(task));
        return new TaskLifecycleResponse(task.getId(), task.getStatus());
    }

    @Transactional
    public TaskLifecycleResponse publish(Long ownerId, Long taskId) {
        Task task = loadOwnedTask(ownerId, taskId);
        requireStatus(task, Set.of(TaskStatus.DRAFT));
        validatePublishRequirements(task);
        task.setStatus(TaskStatus.PUBLISHED);
        task.setPublishedAt(LocalDateTime.now());
        return updateStatus(task, ownerId, "TASK_PUBLISHED", TaskStatus.DRAFT);
    }

    @Transactional
    public TaskLifecycleResponse pause(Long ownerId, Long taskId) {
        Task task = loadOwnedTask(ownerId, taskId);
        requireStatus(task, Set.of(TaskStatus.PUBLISHED));
        task.setStatus(TaskStatus.PAUSED);
        return updateStatus(task, ownerId, "TASK_PAUSED", TaskStatus.PUBLISHED);
    }

    @Transactional
    public TaskLifecycleResponse resume(Long ownerId, Long taskId) {
        Task task = loadOwnedTask(ownerId, taskId);
        requireStatus(task, Set.of(TaskStatus.PAUSED));
        task.setStatus(TaskStatus.PUBLISHED);
        return updateStatus(task, ownerId, "TASK_RESUMED", TaskStatus.PAUSED);
    }

    @Transactional
    public TaskLifecycleResponse end(Long ownerId, Long taskId) {
        Task task = loadOwnedTask(ownerId, taskId);
        requireStatus(task, Set.of(TaskStatus.PUBLISHED, TaskStatus.PAUSED));
        TaskStatus beforeStatus = task.getStatus();
        task.setStatus(TaskStatus.ENDED);
        task.setEndedAt(LocalDateTime.now());
        return updateStatus(task, ownerId, "TASK_ENDED", beforeStatus);
    }

    private TaskLifecycleResponse updateStatus(Task task, Long ownerId, String action, TaskStatus beforeStatus) {
        Map<String, Object> beforeJson = Map.of("status", beforeStatus);
        taskMapper.updateById(task);
        appendAudit(task, ownerId, action, beforeJson, Map.of("status", task.getStatus()));
        publishTaskStatusChanged(task, beforeStatus);
        return new TaskLifecycleResponse(task.getId(), task.getStatus());
    }

    private void publishTaskStatusChanged(Task task, TaskStatus beforeStatus) {
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(
                    new com.labelhub.modules.review.event.TaskStatusChangedEvent(
                            this, task.getId(), task.getTitle(),
                            beforeStatus.name(), task.getStatus().name(),
                            LocalDateTime.now()));
        }
    }

    private void requireStatus(Task task, Set<TaskStatus> allowedStatuses) {
        if (!allowedStatuses.contains(task.getStatus())) {
            throw new BusinessException(TASK_STATUS_NOT_ALLOWED, "任务状态不允许这样流转");
        }
    }

    private void validatePublishRequirements(Task task) {
        if (task.getQuota() == null || task.getQuota() <= 0) {
            throw missingPublishRequirement("任务配额不能为空");
        }
        if (!Integer.valueOf(SINGLE_LABELER_OVERLAP_COUNT).equals(task.getOverlapCount())) {
            throw missingPublishRequirement("任务重叠标注数量必须为 1");
        }
        if (task.getDeadlineAt() == null || !task.getDeadlineAt().isAfter(LocalDateTime.now())) {
            throw missingPublishRequirement("任务截止时间必须晚于当前时间");
        }
        if (!publishDependencyChecker.datasetReady(task.getId())) {
            throw missingPublishRequirement("任务数据集不能为空");
        }
//        if (!publishDependencyChecker.templateVersionExists(task.getPublishedTemplateVersionId())) {
//            throw missingPublishRequirement("任务模板版本不能为空");
//        }
        if (!publishDependencyChecker.aiReviewConfigExists(task.getId(), task.getAiReviewConfigId())) {
            throw missingPublishRequirement("任务 AI 审核配置不能为空");
        }
        if (!publishDependencyChecker.rewardRuleExists(task.getId())) {
            throw missingPublishRequirement("任务奖励规则不能为空");
        }
    }

    private BusinessException missingPublishRequirement(String message) {
        return new BusinessException(TASK_PUBLISH_REQUIREMENT_MISSING, message);
    }

    private TaskDetailResponse toDetailResponse(Task task) {
        AiReviewConfigResponse aiReviewConfig = findAiReviewConfig(task);
        LlmProviderResponse aiProvider = findAiProvider(aiReviewConfig);
        return new TaskDetailResponse(
                task.getId(),
                task.getOwnerId(),
                task.getTitle(),
                task.getDescription(),
                task.getInstructionRichText(),
                task.getStatus(),
                listTags(task.getId()),
                task.getQuota(),
                task.getClaimedCount(),
                task.getOverlapCount(),
                task.getDeadlineAt(),
                task.getPublishedTemplateVersionId(),
                task.getAiReviewConfigId(),
                task.getReviewLevelCount(),
                task.getRewardVisible(),
                task.getPublishedAt(),
                task.getEndedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                aiProvider,
                aiReviewConfig
        );
    }

    private AiReviewConfigResponse findAiReviewConfig(Task task) {
        if (task.getAiReviewConfigId() == null) {
            return null;
        }
        return aiReviewConfigService.findResponseByTaskId(task.getId()).orElse(null);
    }

    private LlmProviderResponse findAiProvider(AiReviewConfigResponse aiReviewConfig) {
        if (aiReviewConfig == null || aiReviewConfig.providerId() == null) {
            return null;
        }
        return llmProviderService.findResponseById(aiReviewConfig.providerId()).orElse(null);
    }

    private List<String> listTags(Long taskId) {
        return taskTagMapper.selectList(new QueryWrapper<TaskTag>()
                        .eq("task_id", taskId)
                        .orderByAsc("id"))
                .stream()
                .map(TaskTag::getTagName)
                .toList();
    }

    private void appendAudit(Task task,
                             Long actorId,
                             String action,
                             Map<String, Object> beforeJson,
                             Map<String, Object> afterJson) {
        auditAppender.append(new AuditCommand(USER_ACTOR_TYPE, actorId, TASK_BIZ_TYPE, task.getId(), action, beforeJson, afterJson,
                traceIdProvider.currentTraceId(), null));
    }

    private Map<String, Object> snapshot(Task task) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", task.getId());
        snapshot.put("ownerId", task.getOwnerId());
        snapshot.put("title", task.getTitle());
        snapshot.put("status", task.getStatus());
        snapshot.put("quota", task.getQuota());
        snapshot.put("overlapCount", task.getOverlapCount());
        snapshot.put("deadlineAt", task.getDeadlineAt());
        snapshot.put("publishedTemplateVersionId", task.getPublishedTemplateVersionId());
        snapshot.put("aiReviewConfigId", task.getAiReviewConfigId());
        return snapshot;
    }

    private Task loadOwnedTask(Long ownerId, Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || (!CurrentUserContext.isAdmin() && !ownerId.equals(task.getOwnerId()))) {
            throw new BusinessException(TASK_NOT_FOUND, "任务不存在");
        }
        return task;
    }

    private void replaceTags(Long taskId, Iterable<String> tags) {
        if (tags == null) {
            return;
        }
        LinkedHashSet<String> normalizedTags = new LinkedHashSet<>();
        tags.forEach(tag -> {
            if (tag != null && !tag.isBlank()) {
                normalizedTags.add(tag.trim());
            }
        });
        normalizedTags.stream()
                .map(tag -> toTaskTag(taskId, tag))
                .filter(Objects::nonNull)
                .forEach(taskTagMapper::insert);
    }

    private boolean hasAiInlineConfig(CreateTaskRequest request) {
        return request.aiProviderId() != null
                && request.aiPrompt() != null && !request.aiPrompt().isBlank()
                && request.aiScoringDimensions() != null && !request.aiScoringDimensions().isEmpty()
                && request.aiPassThreshold() != null
                && request.aiManualReviewThreshold() != null;
    }

    private TaskTag toTaskTag(Long taskId, String tagName) {
        TaskTag taskTag = new TaskTag();
        taskTag.setTaskId(taskId);
        taskTag.setTagName(tagName);
        return taskTag;
    }
}
