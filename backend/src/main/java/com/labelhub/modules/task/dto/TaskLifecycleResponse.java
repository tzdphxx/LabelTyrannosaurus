package com.labelhub.modules.task.dto;

import com.labelhub.modules.task.domain.TaskStatus;

public record TaskLifecycleResponse(Long taskId, TaskStatus status) {
}
