package com.labelhub.modules.task.dto;

import com.labelhub.modules.task.domain.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;

public record OwnerTaskSummaryResponse(
        Long taskId,
        String title,
        TaskStatus status,
        List<String> tags,
        Integer quota,
        Integer claimedCount,
        Integer overlapCount,
        LocalDateTime deadlineAt,
        LocalDateTime publishedAt,
        LocalDateTime endedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
