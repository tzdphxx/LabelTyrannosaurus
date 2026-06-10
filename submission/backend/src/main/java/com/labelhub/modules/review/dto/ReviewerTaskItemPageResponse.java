package com.labelhub.modules.review.dto;

import com.labelhub.common.api.PageResponse;

public record ReviewerTaskItemPageResponse(
        Long taskId,
        String taskTitle,
        String taskStatus,
        long totalItemCount,
        ReviewerTaskStatusSummary statusSummary,
        PageResponse<ReviewerTaskItemRow> page
) {
}
