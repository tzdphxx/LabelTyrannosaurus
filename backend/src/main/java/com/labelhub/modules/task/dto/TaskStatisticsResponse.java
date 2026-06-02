package com.labelhub.modules.task.dto;

public record TaskStatisticsResponse(
        Long taskId,
        int totalItems,
        int claimedCount,
        int submittedCount,
        int approvedCount,
        int rejectedCount,
        int pendingReviewCount,
        String passRate
) {
}
