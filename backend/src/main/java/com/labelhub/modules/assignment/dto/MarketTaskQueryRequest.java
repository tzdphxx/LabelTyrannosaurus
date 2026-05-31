package com.labelhub.modules.assignment.dto;

import com.labelhub.modules.task.domain.TaskStatus;

public record MarketTaskQueryRequest(String keyword, String tag, TaskStatus status) {
}
