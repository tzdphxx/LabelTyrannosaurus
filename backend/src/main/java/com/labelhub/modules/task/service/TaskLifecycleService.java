package com.labelhub.modules.task.service;

import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.domain.TaskTag;
import com.labelhub.modules.task.dto.CreateTaskRequest;
import com.labelhub.modules.task.dto.TaskLifecycleResponse;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.task.mapper.TaskTagMapper;
import java.util.LinkedHashSet;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskLifecycleService {

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
