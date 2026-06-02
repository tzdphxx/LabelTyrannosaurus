package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.submission.domain.SubmissionStatus;

@Schema(description = "审核人提交列表项")
public record ReviewerSubmissionListItem(
        @Schema(description = "提交ID") Long submissionId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "数据集条目ID") Long datasetItemId,
        @Schema(description = "标注人ID") Long labelerId,
        @Schema(description = "提交状态") SubmissionStatus submissionStatus,
        @Schema(description = "AI审核状态") AiReviewStatus aiReviewStatus,
        @Schema(description = "AI决策") String aiDecision,
        @Schema(description = "冲突状态") String conflictStatus,
        @Schema(description = "审核级别") Integer reviewLevel,
        @Schema(description = "分配的审核人ID") Long assignedReviewerId,
        @Schema(description = "创建时间") java.time.LocalDateTime createdAt,
        @Schema(description = "更新时间") java.time.LocalDateTime updatedAt
) {
}
