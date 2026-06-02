package com.labelhub.modules.submission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "标注人提交详情响应")
public record LabelerSubmissionDetailResponse(
        @Schema(description = "提交ID") Long submissionId,
        @Schema(description = "分配ID") Long assignmentId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "数据集条目ID") Long datasetItemId,
        @Schema(description = "模板版本ID") Long templateVersionId,
        @Schema(description = "版本号") Integer versionNo,
        @Schema(description = "提交状态") SubmissionStatus submissionStatus,
        @Schema(description = "分配状态") AssignmentStatus assignmentStatus,
        @Schema(description = "条目JSON") String itemJson,
        @Schema(description = "模板Schema JSON") String schemaJson,
        @Schema(description = "答案JSON") String answerJson,
        @Schema(description = "AI审核状态") AiReviewStatus aiReviewStatus,
        @Schema(description = "AI决策") String aiDecision,
        @Schema(description = "AI建议") String aiSuggestion,
        @Schema(description = "驳回原因") String rejectReason,
        @Schema(description = "审核记录列表") List<ReviewRecordSummary> reviewRecords,
        @Schema(description = "版本历史列表") List<VersionSummary> versionHistory,
        @Schema(description = "是否可修改") boolean canModify
) {
    @Schema(description = "审核记录摘要")
    public record ReviewRecordSummary(
            @Schema(description = "审核记录ID") Long reviewRecordId,
            @Schema(description = "操作") String action,
            @Schema(description = "原因") String reason,
            @Schema(description = "创建时间") LocalDateTime createdAt
    ) {}

    @Schema(description = "版本摘要")
    public record VersionSummary(
            @Schema(description = "提交ID") Long submissionId,
            @Schema(description = "版本号") Integer versionNo,
            @Schema(description = "提交状态") SubmissionStatus status,
            @Schema(description = "创建时间") LocalDateTime createdAt
    ) {}
}
