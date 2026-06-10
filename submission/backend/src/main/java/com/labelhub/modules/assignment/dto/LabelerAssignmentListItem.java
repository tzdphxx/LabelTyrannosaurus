package com.labelhub.modules.assignment.dto;

import com.labelhub.modules.assignment.domain.AssignmentStatus;
import java.time.LocalDateTime;

public record LabelerAssignmentListItem(
        Long assignmentId,
        Long taskId,
        String taskTitle,
        Long datasetItemId,
        AssignmentStatus status,
        Integer draftVersion,
        LocalDateTime claimedAt,
        LocalDateTime returnedAt,
        LocalDateTime updatedAt
) {
}
