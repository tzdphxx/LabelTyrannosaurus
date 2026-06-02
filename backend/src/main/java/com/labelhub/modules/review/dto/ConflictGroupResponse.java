package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.modules.review.domain.ConflictStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "冲突组响应")
public record ConflictGroupResponse(
        @Schema(description = "冲突组ID") Long groupId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "数据集条目ID") Long datasetItemId,
        @Schema(description = "冲突状态") ConflictStatus status,
        @Schema(description = "一致性得分") BigDecimal consensusScore,
        @Schema(description = "黄金标准提交ID") Long goldenSubmissionId,
        @Schema(description = "候选提交列表") List<CandidateSubmissionItem> candidateSubmissions,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "解决时间") LocalDateTime resolvedAt
) {
    @Schema(description = "候选提交条目")
    public record CandidateSubmissionItem(
            @Schema(description = "提交ID") Long submissionId,
            @Schema(description = "标注人ID") Long labelerId,
            @Schema(description = "答案JSON") String answerJson,
            @Schema(description = "AI审核摘要") AiReviewSummary aiReviewSummary,
            @Schema(description = "审核记录列表") List<ReviewRecordItem> reviewRecords,
            @Schema(description = "版本号") Integer versionNo
    ) {}

    @Schema(description = "AI审核摘要")
    public record AiReviewSummary(
            @Schema(description = "AI审核结果ID") Long aiReviewResultId,
            @Schema(description = "智能体运行ID") Long agentRunId,
            @Schema(description = "状态") String status,
            @Schema(description = "决策") String decision,
            @Schema(description = "平均得分") String averageScore,
            @Schema(description = "风险标记") String riskFlags,
            @Schema(description = "建议") String suggestion
    ) {}

    @Schema(description = "审核记录条目")
    public record ReviewRecordItem(
            @Schema(description = "审核记录ID") Long reviewRecordId,
            @Schema(description = "审核人ID") Long reviewerId,
            @Schema(description = "操作") String action,
            @Schema(description = "审核级别") Integer reviewLevel,
            @Schema(description = "原因") String reason,
            @Schema(description = "审核评论") String reviewComment,
            @Schema(description = "创建时间") LocalDateTime createdAt
    ) {}
}
