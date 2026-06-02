package com.labelhub.modules.review.dto;

public record ReviewerTaskSummary(
        Long taskId,
        String taskTitle,
        int pendingCount,
        int myPendingCount,
        int totalReviewedCount
) {
}
