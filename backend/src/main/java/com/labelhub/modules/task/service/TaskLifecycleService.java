package com.labelhub.modules.task.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.modules.ai.dto.AiReviewConfigRequest;
import com.labelhub.modules.ai.service.AiReviewConfigService;
import com.labelhub.modules.assignment.mapper.AssignmentDispatchMapper;
import com.labelhub.modules.dataset.dto.DatasetImportJobResponse;
import com.labelhub.modules.dataset.dto.DatasetImportRequest;
import com.labelhub.modules.dataset.service.DatasetImportService;
import com.labelhub.modules.reward.dto.RewardRuleResponse;
import com.labelhub.modules.reward.service.RewardRuleService;
import com.labelhub.modules.task.domain.ClaimStrategy;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.domain.TaskTag;
import com.labelhub.modules.task.dto.CreateTaskRequest;
import com.labelhub.modules.task.dto.CreateTaskResponse;
import com.labelhub.modules.task.dto.TaskResponse;
import com.labelhub.modules.task.dto.TaskStatusResponse;
import com.labelhub.modules.task.dto.TaskSummaryResponse;
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
    private static final int INVALID_STRATEGY = 400103;
    private static final String TASK_BIZ_TYPE = "TASK";
    private static final String USER_ACTOR_TYPE = "USER";

    private final TaskMapper taskMapper;
    private final TaskTagMapper taskTagMapper;
    private final TaskPublishDependencyChecker publishDependencyChecker;
    private final AuditAppender auditAppender;
    private final TraceIdProvider traceIdProvider;
    private final DatasetImportService datasetImportService;
    private final AiReviewConfigService aiReviewConfigService;
    private final RewardRuleService rewardRuleService;
    private final AssignmentDispatchMapper dispatchMapper;
    private final org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    public TaskLifecycleService(TaskMapper taskMapper,
                                TaskTagMapper taskTagMapper,
                                TaskPublishDependencyChecker publishDependencyChecker,
                                AuditAppender auditAppender,
                                TraceIdProvider traceIdProvider,
                                DatasetImportService datasetImportService,
                                AiReviewConfigService aiReviewConfigService,
                                RewardRuleService rewardRuleService,
                                AssignmentDispatchMapper dispatchMapper,
                                org.springframework.context.ApplicationEventPublisher applicationEventPublisher) {
        this.taskMapper = taskMapper;
        this.taskTagMapper = taskTagMapper;
        this.publishDependencyChecker = publishDependencyChecker;
        this.auditAppender = auditAppender;
        this.traceIdProvider = traceIdProvider;
        this.datasetImportService = datasetImportService;
        this.aiReviewConfigService = aiReviewConfigService;
        this.rewardRuleService = rewardRuleService;
        this.dispatchMapper = dispatchMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public List<TaskSummaryResponse> listOwnerTasks(Long ownerId) {
        return taskMapper.selectList(new QueryWrapper<Task>()
                        .eq("owner_id", ownerId)
                        .orderByDesc("updated_at")
                        .orderByDesc("id"))
                .stream()
                .map(task -> new TaskSummaryResponse(
                        task.getId(),
                        task.getTitle(),
                        task.getStatus(),
                        listTags(task.getId()),
                        task.getQuota(),
                        task.getClaimedCount(),
                        task.getOverlapCount(),
                        task.getStrategy(),
                        task.getDeadlineAt(),
                        task.getPublishedAt(),
                        task.getEndedAt(),
                        task.getCreatedAt(),
                        task.getUpdatedAt()
                ))
                .toList();
    }

    public TaskResponse getOwnedTask(Long ownerId, Long taskId) {
        return toDetailResponse(loadOwnedTask(ownerId, taskId));
    }

    @Transactional
    public TaskStatusResponse create(Long ownerId, CreateTaskRequest request) {
        return createTask(ownerId, request).lifecycleResponse();
    }

    /**
     * 创建任务（含数据集导入和奖励规则）。
     * controller 统一调用此方法，返回包含 taskId、status、datasetImportJob 和 rewardRule 的完整响应。
     */
    @Transactional
    public CreateTaskResponse createWithDataset(Long ownerId, CreateTaskRequest request) {
        CreatedTaskResult createdTask = createTask(ownerId, request);
        DatasetImportJobResponse importJob = null;
        if (request.datasetFileId() != null) {
            importJob = datasetImportService.createAppendImport(
                    createdTask.lifecycleResponse().taskId(),
                    new DatasetImportRequest(request.datasetFileId())
            );
        }
        return new CreateTaskResponse(
                createdTask.lifecycleResponse().taskId(),
                createdTask.lifecycleResponse().status(),
                importJob,
                createdTask.rewardRule()
        );
    }

    /**
     * 创建任务核心逻辑：
     * 1. 插入任务（含内联 AI 配置子流程）
     * 2. 如有 rewardRule 则创建奖励规则并回写 rewardVisible
     * 3. 替换标签、记录审计快照
     */
    private CreatedTaskResult createTask(Long ownerId, CreateTaskRequest request) {
        Task task = new Task();
        task.setOwnerId(ownerId);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setInstructionRichText(request.instructionRichText());
        task.setStatus(TaskStatus.DRAFT);
        task.setQuota(resolveQuota(request.strategy(), request.quota()));
        task.setClaimedCount(0);
        task.setOverlapCount(request.overlapCount());
        task.setStrategy(parseStrategy(request.strategy()));
        task.setMaxClaimsPerLabeler(request.maxClaimsPerLabeler());
        task.setDeadlineAt(request.deadlineAt());
        task.setPublishedTemplateVersionId(request.publishedTemplateVersionId());
        task.setAiReviewConfigId(request.aiReviewConfigId());
        task.setReviewLevelCount(request.reviewLevelCount() != null ? request.reviewLevelCount() : 1);
        task.setRewardVisible(request.rewardRule() == null
                || request.rewardRule().rewardVisible() == null
                || request.rewardRule().rewardVisible());
        taskMapper.insert(task);

        if (hasAiInlineConfig(request)) {
            AiReviewConfigRequest aiRequest = new AiReviewConfigRequest(
                    request.aiProviderId(),
                    request.aiModelName(),
                    request.aiPrompt(),
                    request.aiScoringDimensions(),
                    request.aiPassThreshold(),
                    request.aiManualReviewThreshold(),
                    null, null, null, null, null, null, null,
                    request.aiReviewStrategy());
            var aiConfig = aiReviewConfigService.save(ownerId, task.getId(), aiRequest);
            task.setAiReviewConfigId(aiConfig.id());
            taskMapper.updateById(task);
        }

        RewardRuleResponse rewardRule = null;
        if (request.rewardRule() != null) {
            rewardRule = rewardRuleService.saveRuleForTaskOwner(task.getId(), ownerId, request.rewardRule());
            task.setRewardVisible(rewardRule.rewardVisible());
            taskMapper.updateById(task);
        }

        replaceTags(task.getId(), request.tags());
        appendAudit(task, ownerId, "TASK_CREATED", null, snapshot(task));
        return new CreatedTaskResult(new TaskStatusResponse(task.getId(), task.getStatus()), rewardRule);
    }

    @Transactional
    public TaskStatusResponse updateDraft(Long ownerId, Long taskId, UpdateTaskRequest request) {
        Task task = loadOwnedTask(ownerId, taskId);
        if (task.getStatus() != TaskStatus.DRAFT) {
            throw new BusinessException(TASK_STATUS_NOT_ALLOWED, "只有草稿状态的任务可以编辑");
        }
        Map<String, Object> beforeJson = snapshot(task);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setInstructionRichText(request.instructionRichText());
        task.setQuota(resolveQuota(request.strategy(), request.quota()));
        task.setOverlapCount(request.overlapCount());
        if (request.strategy() != null) {
            task.setStrategy(parseStrategy(request.strategy()));
        }
        if (request.maxClaimsPerLabeler() != null) {
            task.setMaxClaimsPerLabeler(request.maxClaimsPerLabeler());
        }
        task.setDeadlineAt(request.deadlineAt());
        task.setPublishedTemplateVersionId(request.publishedTemplateVersionId());
        task.setAiReviewConfigId(request.aiReviewConfigId());
        if (request.reviewLevelCount() != null) {
            task.setReviewLevelCount(request.reviewLevelCount());
        }
        if (request.rewardRule() != null) {
            RewardRuleResponse rewardRule = rewardRuleService.saveRuleForTaskOwner(taskId, ownerId, request.rewardRule());
            task.setRewardVisible(rewardRule.rewardVisible());
        }
        taskMapper.updateById(task);
        taskTagMapper.delete(new QueryWrapper<TaskTag>().eq("task_id", taskId));
        replaceTags(taskId, request.tags());
        appendAudit(task, ownerId, "TASK_UPDATED", beforeJson, snapshot(task));
        return new TaskStatusResponse(task.getId(), task.getStatus());
    }

    @Transactional
    public TaskStatusResponse publish(Long ownerId, Long taskId) {
        Task task = loadOwnedTask(ownerId, taskId);
        requireStatus(task, Set.of(TaskStatus.DRAFT));
        validatePublishRequirements(task);
        task.setStatus(TaskStatus.PUBLISHED);
        task.setPublishedAt(LocalDateTime.now());
        return updateStatus(task, ownerId, "TASK_PUBLISHED", TaskStatus.DRAFT);
    }

    @Transactional
    public TaskStatusResponse pause(Long ownerId, Long taskId) {
        Task task = loadOwnedTask(ownerId, taskId);
        requireStatus(task, Set.of(TaskStatus.PUBLISHED));
        task.setStatus(TaskStatus.PAUSED);
        return updateStatus(task, ownerId, "TASK_PAUSED", TaskStatus.PUBLISHED);
    }

    @Transactional
    public TaskStatusResponse resume(Long ownerId, Long taskId) {
        Task task = loadOwnedTask(ownerId, taskId);
        requireStatus(task, Set.of(TaskStatus.PAUSED));
        task.setStatus(TaskStatus.PUBLISHED);
        return updateStatus(task, ownerId, "TASK_RESUMED", TaskStatus.PAUSED);
    }

    @Transactional
    public TaskStatusResponse end(Long ownerId, Long taskId) {
        Task task = loadOwnedTask(ownerId, taskId);
        requireStatus(task, Set.of(TaskStatus.PUBLISHED, TaskStatus.PAUSED));
        TaskStatus beforeStatus = task.getStatus();
        task.setStatus(TaskStatus.ENDED);
        task.setEndedAt(LocalDateTime.now());
        return updateStatus(task, ownerId, "TASK_ENDED", beforeStatus);
    }

    private TaskStatusResponse updateStatus(Task task, Long ownerId, String action, TaskStatus beforeStatus) {
        Map<String, Object> beforeJson = Map.of("status", beforeStatus);
        taskMapper.updateById(task);
        appendAudit(task, ownerId, action, beforeJson, Map.of("status", task.getStatus()));
        publishTaskStatusChanged(task, beforeStatus);
        return new TaskStatusResponse(task.getId(), task.getStatus());
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
        if (task.getOverlapCount() == null || task.getOverlapCount() < 1) {
            throw missingPublishRequirement("Task overlap count is required");
        }
        if (task.getDeadlineAt() == null || !task.getDeadlineAt().isAfter(LocalDateTime.now())) {
            throw missingPublishRequirement("任务截止时间必须晚于当前时间");
        }
        if (!publishDependencyChecker.datasetReady(task.getId())) {
            throw missingPublishRequirement("任务数据集不能为空");
        }
        if (!publishDependencyChecker.templateVersionExists(task.getPublishedTemplateVersionId())) {
            throw missingPublishRequirement("Task template version is required");
        }
        if (!publishDependencyChecker.aiReviewConfigExists(task.getId(), task.getAiReviewConfigId())) {
            throw missingPublishRequirement("任务 AI 审核配置不能为空");
        }
        if (!publishDependencyChecker.rewardRuleExists(task.getId())) {
            throw missingPublishRequirement("任务奖励规则不能为空");
        }
        ClaimStrategy strategy = task.getStrategy();
        if (strategy == null) {
            strategy = ClaimStrategy.FCFS;
        }
        if (strategy != ClaimStrategy.ASSIGNED
                && (task.getQuota() == null || task.getQuota() <= 0)) {
            throw missingPublishRequirement("Task quota is required");
        }
        if (strategy == ClaimStrategy.ASSIGNED) {
            int dispatchCount = dispatchMapper.countByTaskId(task.getId());
            if (dispatchCount == 0) {
                throw missingPublishRequirement(
                        "At least one dispatch is required for ASSIGNED strategy");
            }
            task.setQuota(dispatchCount);
        }
    }

    private BusinessException missingPublishRequirement(String message) {
        return new BusinessException(TASK_PUBLISH_REQUIREMENT_MISSING, message);
    }

    private TaskResponse toDetailResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                listTags(task.getId()),
                task.getQuota(),
                task.getClaimedCount(),
                task.getOverlapCount(),
                task.getStrategy(),
                task.getDeadlineAt(),
                task.getPublishedAt(),
                task.getEndedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getOwnerId(),
                task.getDescription(),
                task.getInstructionRichText(),
                task.getMaxClaimsPerLabeler(),
                task.getPublishedTemplateVersionId(),
                aiReviewConfigService.findResponseByTaskId(task.getId()),
                task.getReviewLevelCount(),
                task.getRewardVisible(),
                rewardRuleService.findLatestRule(task.getId())
        );
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
        snapshot.put("strategy", task.getStrategy());
        snapshot.put("maxClaimsPerLabeler", task.getMaxClaimsPerLabeler());
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

    private ClaimStrategy parseStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return ClaimStrategy.FCFS;
        }
        try {
            return ClaimStrategy.valueOf(strategy.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(INVALID_STRATEGY, "Invalid claim strategy: " + strategy);
        }
    }

    private int resolveQuota(String strategy, Integer requestQuota) {
        ClaimStrategy parsed = parseStrategy(strategy);
        if (parsed == ClaimStrategy.ASSIGNED) {
            return 0;
        }
        if (requestQuota == null || requestQuota <= 0) {
            throw new BusinessException(TASK_PUBLISH_REQUIREMENT_MISSING,
                    "Quota is required for " + parsed.name());
        }
        return requestQuota;
    }

    /** 创建任务的结果封装，避免 createTask 返回多个裸值。 */
    private record CreatedTaskResult(TaskStatusResponse lifecycleResponse, RewardRuleResponse rewardRule) {
    }
}
