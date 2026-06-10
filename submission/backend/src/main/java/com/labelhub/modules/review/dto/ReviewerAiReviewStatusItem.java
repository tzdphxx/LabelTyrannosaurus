package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.submission.domain.SubmissionStatus;

@Schema(description = "审查员 AI 预审状态项")
public record ReviewerAiReviewStatusItem(
        @Schema(description = "提交ID") Long submissionId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "任务标题") String taskTitle,
        @Schema(description = "提交状态") SubmissionStatus submissionStatus,
        @Schema(description = "AI 审核状态") AiReviewStatus aiReviewStatus,
        @Schema(description = "AI 决策（PASS / REJECT / MANUAL_REVIEW）") String aiDecision,
        @Schema(description = "AI 平均评分") String averageScore,
        @Schema(description = "是否分配给当前审查员") Boolean assignedToMe,
        @Schema(description = "提交时间") java.time.LocalDateTime submittedAt
) {
}
