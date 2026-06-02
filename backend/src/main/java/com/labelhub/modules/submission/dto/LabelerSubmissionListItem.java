package com.labelhub.modules.submission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import java.time.LocalDateTime;

@Schema(description = "标注人提交列表项")
public record LabelerSubmissionListItem(
        @Schema(description = "提交ID") Long submissionId,
        @Schema(description = "分配ID") Long assignmentId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "数据集条目ID") Long datasetItemId,
        @Schema(description = "版本号") Integer versionNo,
        @Schema(description = "提交状态") SubmissionStatus submissionStatus,
        @Schema(description = "分配状态") AssignmentStatus assignmentStatus,
        @Schema(description = "AI审核状态") AiReviewStatus aiReviewStatus,
        @Schema(description = "AI决策") String aiDecision,
        @Schema(description = "审核摘要") String reviewSummary,
        @Schema(description = "驳回原因") String rejectReason,
        @Schema(description = "是否为黄金标准") Boolean isGolden,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
}
