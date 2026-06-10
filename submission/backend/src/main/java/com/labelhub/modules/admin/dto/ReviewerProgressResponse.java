package com.labelhub.modules.admin.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReviewerProgressResponse(
        Long reviewerId,
        String username,
        String email,
        Boolean enabled,
        Boolean loginEnabled,
        Long pendingCount,
        Long todayReviewedCount,
        Long totalReviewedCount,
        BigDecimal approvalRate,
        Long claimedTaskCount,
        List<ClaimedReviewTaskResponse> claimedTasks
) {
    public ReviewerProgressResponse withClaimedTasks(List<ClaimedReviewTaskResponse> tasks) {
        List<ClaimedReviewTaskResponse> safeTasks = tasks == null ? List.of() : List.copyOf(tasks);
        return new ReviewerProgressResponse(reviewerId, username, email, enabled, loginEnabled,
                pendingCount, todayReviewedCount, totalReviewedCount, approvalRate,
                (long) safeTasks.size(), safeTasks);
    }
}
