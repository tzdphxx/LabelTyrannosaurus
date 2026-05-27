package com.labelhub.modules.task.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.domain.TaskTag;
import com.labelhub.modules.task.dto.CreateTaskRequest;
import com.labelhub.modules.task.dto.TaskLifecycleResponse;
import com.labelhub.modules.task.dto.UpdateTaskRequest;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.task.mapper.TaskTagMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
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
    private static final String TASK_BIZ_TYPE = "TASK";
    private static final String USER_ACTOR_TYPE = "USER";

    private final TaskMapper taskMapper;
    private final TaskTagMapper taskTagMapper;
    private final TaskPublishDependencyChecker publishDependencyChecker;
    private final AuditAppender auditAppender;

    public TaskLifecycleService(TaskMapper taskMapper,
                                TaskTagMapper taskTagMapper,
                                TaskPublishDependencyChecker publishDependencyChecker,
                                AuditAppender auditAppender) {
        this.taskMapper = taskMapper;
        this.taskTagMapper = taskTagMapper;
        this.publishDependencyChecker = publishDependencyChecker;
        this.auditAppender = auditAppender;
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
        task.setOverlapCount(request.overlapCount());
        task.setDeadlineAt(request.deadlineAt());
        task.setPublishedTemplateVersionId(request.publishedTemplateVersionId());
        task.setAiReviewConfigId(request.aiReviewConfigId());
        task.setRewardVisible(true);
        taskMapper.insert(task);
        replaceTags(task.getId(), request.tags());
        appendAudit(task, ownerId, "TASK_CREATED", null, snapshot(task));
        return new TaskLifecycleResponse(task.getId(), task.getStatus());
    }

    @Transactional
    public TaskLifecycleResponse updateDraft(Long ownerId, Long taskId, UpdateTaskRequest request) {
        Task task = loadOwnedTask(ownerId, taskId);
        if (task.getStatus() != TaskStatus.DRAFT) {
            throw new BusinessException(TASK_STATUS_NOT_ALLOWED, "Only draft tasks can be edited");
        }
        Map<String, Object> beforeJson = snapshot(task);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setInstructionRichText(request.instructionRichText());
        task.setQuota(request.quota());
        task.setOverlapCount(request.overlapCount());
        task.setDeadlineAt(request.deadlineAt());
        task.setPublishedTemplateVersionId(request.publishedTemplateVersionId());
        task.setAiReviewConfigId(request.aiReviewConfigId());
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
        return new TaskLifecycleResponse(task.getId(), task.getStatus());
    }

    private void requireStatus(Task task, Set<TaskStatus> allowedStatuses) {
        if (!allowedStatuses.contains(task.getStatus())) {
            throw new BusinessException(TASK_STATUS_NOT_ALLOWED, "Task status transition is not allowed");
        }
    }

    private void validatePublishRequirements(Task task) {
        if (task.getQuota() == null || task.getQuota() <= 0) {
            throw missingPublishRequirement("Task quota is required");
        }
        if (task.getOverlapCount() == null || task.getOverlapCount() < 1) {
            throw missingPublishRequirement("Task overlap count is required");
        }
        if (task.getDeadlineAt() == null || !task.getDeadlineAt().isAfter(LocalDateTime.now())) {
            throw missingPublishRequirement("Task deadline must be in the future");
        }
        if (!publishDependencyChecker.datasetReady(task.getId())) {
            throw missingPublishRequirement("Task dataset is required");
        }
        if (!publishDependencyChecker.templateVersionExists(task.getPublishedTemplateVersionId())) {
            throw missingPublishRequirement("Task template version is required");
        }
        if (task.getAiReviewConfigId() == null) {
            throw missingPublishRequirement("Task AI review config is required");
        }
        if (!publishDependencyChecker.rewardRuleExists(task.getId())) {
            throw missingPublishRequirement("Task reward rule is required");
        }
    }

    private BusinessException missingPublishRequirement(String message) {
        return new BusinessException(TASK_PUBLISH_REQUIREMENT_MISSING, message);
    }

    private void appendAudit(Task task,
                             Long actorId,
                             String action,
                             Map<String, Object> beforeJson,
                             Map<String, Object> afterJson) {
        auditAppender.append(TASK_BIZ_TYPE, task.getId(), USER_ACTOR_TYPE, actorId, action, beforeJson, afterJson, null, null);
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
        if (task == null || !task.getOwnerId().equals(ownerId)) {
            throw new BusinessException(TASK_NOT_FOUND, "Task not found");
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

    private TaskTag toTaskTag(Long taskId, String tagName) {
        TaskTag taskTag = new TaskTag();
        taskTag.setTaskId(taskId);
        taskTag.setTagName(tagName);
        return taskTag;
    }
}
