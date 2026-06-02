package com.labelhub.modules.assignment.dto;

import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.dataset.domain.DatasetItemStatus;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "标注任务详情响应")
public record AssignmentDetailResponse(
        @Schema(description = "分配ID") Long assignmentId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "数据集条目ID") Long datasetItemId,
        @Schema(description = "模板版本ID") Long templateVersionId,
        @Schema(description = "分配状态") AssignmentStatus assignmentStatus,
        @Schema(description = "题目状态") DatasetItemStatus datasetItemStatus,
        @Schema(description = "模板Schema JSON") String schemaJson,
        @Schema(description = "条目JSON") String itemJson,
        @Schema(description = "草稿答案JSON") String draftAnswerJson,
        @Schema(description = "草稿版本") Integer draftVersion,
        @Schema(description = "最新提交ID") Long latestSubmissionId,
        @Schema(description = "最新提交状态") SubmissionStatus latestSubmissionStatus,
        @Schema(description = "退回原因") String returnedReason,
        @Schema(description = "退回时间") LocalDateTime returnedAt,
        @Schema(description = "奖励摘要") RewardSummaryResponse rewardSummary,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
}
