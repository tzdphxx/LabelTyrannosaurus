package com.labelhub.modules.task.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.domain.TaskTag;
import com.labelhub.modules.task.dto.CreateTaskRequest;
import com.labelhub.modules.task.dto.TaskLifecycleResponse;
import com.labelhub.modules.task.dto.UpdateTaskRequest;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.task.mapper.TaskTagMapper;
import java.util.LinkedHashSet;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskLifecycleService {

    private static final int TASK_NOT_FOUND = 404001;
    private static final int TASK_STATUS_NOT_ALLOWED = 400101;

    private final TaskMapper taskMapper;
    private final TaskTagMapper taskTagMapper;

    public TaskLifecycleService(TaskMapper taskMapper, TaskTagMapper taskTagMapper) {
        this.taskMapper = taskMapper;
        this.taskTagMapper = taskTagMapper;
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
        task.setRewardVisible(true);
        taskMapper.insert(task);
        replaceTags(task.getId(), request.tags());
        return new TaskLifecycleResponse(task.getId(), task.getStatus());
    }

    @Transactional
    public TaskLifecycleResponse updateDraft(Long ownerId, Long taskId, UpdateTaskRequest request) {
        Task task = loadOwnedTask(ownerId, taskId);
        if (task.getStatus() != TaskStatus.DRAFT) {
            throw new BusinessException(TASK_STATUS_NOT_ALLOWED, "Only draft tasks can be edited");
        }
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setInstructionRichText(request.instructionRichText());
        task.setQuota(request.quota());
        task.setOverlapCount(request.overlapCount());
        task.setDeadlineAt(request.deadlineAt());
        taskMapper.updateById(task);
        taskTagMapper.delete(new QueryWrapper<TaskTag>().eq("task_id", taskId));
        replaceTags(taskId, request.tags());
        return new TaskLifecycleResponse(task.getId(), task.getStatus());
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
