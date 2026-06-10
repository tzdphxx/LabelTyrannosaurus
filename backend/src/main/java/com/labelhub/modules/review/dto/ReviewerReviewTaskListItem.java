package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Reviewer task marketplace item")
public record ReviewerReviewTaskListItem(
        @Schema(description = "Task ID", example = "10")
        Long taskId,
        @Schema(description = "Task title")
        String taskTitle,
        @Schema(description = "Task status", example = "PUBLISHED")
        String taskStatus,
        @Schema(description = "Task deadline")
        LocalDateTime deadlineAt,
        @Schema(description = "Pending final submissions for this task")
        int pendingCount,
        @Schema(description = "Pending final submissions assigned to current reviewer")
        int myPendingCount,
        @Schema(description = "Submissions reviewed by current reviewer for this task")
        int totalReviewedCount,
        @Schema(description = "Claim status from current reviewer perspective: MINE / CLAIMED / UNCLAIMED")
        String claimStatus,
        @Schema(description = "Whether current reviewer can claim this task")
        boolean claimable,
        @Schema(description = "Whether current reviewer has claimed this task")
        boolean claimedByMe,
        @Schema(description = "Whether any reviewer has claimed this task")
        boolean claimed
) {
}
