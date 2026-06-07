package com.labelhub.modules.admin.dto;

import com.labelhub.modules.task.domain.TaskStatus;
import java.time.LocalDateTime;

public record AssignableReviewTaskResponse(
        Long taskId,
        String title,
        TaskStatus status,
        LocalDateTime deadlineAt,
        Integer reviewLevel,
        Long pendingCount,
        Boolean claimed,
        Long claimedReviewerId,
        String claimedReviewerName,
        Boolean available
) {
}
