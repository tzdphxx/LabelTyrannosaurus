package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "审核人提交详情响应")
public record ReviewerSubmissionDetailResponse(
        @Schema(description = "提交ID") Long submissionId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "分配ID") Long assignmentId,
        @Schema(description = "数据集条目ID") Long datasetItemId,
        @Schema(description = "标注人ID") Long labelerId,
        @Schema(description = "版本号") Integer versionNo,
        @Schema(description = "提交状态") SubmissionStatus submissionStatus,
        @Schema(description = "答案JSON") String answerJson,
        @Schema(description = "条目JSON") String itemJson,
        @Schema(description = "模板版本ID") Long templateVersionId,
        @Schema(description = "模板Schema JSON") String schemaJson,
        @Schema(description = "AI审核结果摘要") AiReviewSummary aiReviewResult,
        @Schema(description = "智能体运行摘要") AgentRunSummary agentRunSummary,
        @Schema(description = "审核记录列表") List<ReviewRecordItem> reviewRecords,
        @Schema(description = "版本历史列表") List<VersionHistoryItem> versionHistory,
        @Schema(description = "最新预标注摘要") LatestPreAnnotationSummary latestPreAnnotation
) {
    @Schema(description = "AI审核结果摘要")
    public record AiReviewSummary(
            @Schema(description = "AI审核结果ID") Long aiReviewResultId,
            @Schema(description = "智能体运行ID") Long agentRunId,
            @Schema(description = "AI审核状态") AiReviewStatus status,
            @Schema(description = "决策") String decision,
            @Schema(description = "平均得分") String averageScore,
            @Schema(description = "风险标记") String riskFlags,
            @Schema(description = "建议") String suggestion,
            @Schema(description = "错误码") String errorCode,
            @Schema(description = "提示模式") String promptMode,
            @Schema(description = "是否降级") Boolean degraded,
            @Schema(description = "局限性") String limitations
    ) {}

    @Schema(description = "智能体运行摘要")
    public record AgentRunSummary(
            @Schema(description = "智能体运行ID") Long agentRunId,
            @Schema(description = "智能体类型") String agentType,
            @Schema(description = "模型名称") String modelName,
            @Schema(description = "状态") String status,
            @Schema(description = "开始时间") LocalDateTime startedAt,
            @Schema(description = "完成时间") LocalDateTime finishedAt
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

    @Schema(description = "版本历史条目")
    public record VersionHistoryItem(
            @Schema(description = "提交ID") Long submissionId,
            @Schema(description = "版本号") Integer versionNo,
            @Schema(description = "提交状态") SubmissionStatus status,
            @Schema(description = "是否为黄金标准") Boolean isGolden,
            @Schema(description = "创建时间") LocalDateTime createdAt
    ) {}

    @Schema(description = "最新预标注摘要")
    public record LatestPreAnnotationSummary(
            @Schema(description = "预标注ID") Long preAnnotationId,
            @Schema(description = "智能体运行ID") Long agentRunId,
            @Schema(description = "状态") String status,
            @Schema(description = "建议答案JSON") String suggestedAnswerJson,
            @Schema(description = "字段建议") String fieldSuggestions,
            @Schema(description = "风险标记") String riskFlags,
            @Schema(description = "整体置信度") String overallConfidence,
            @Schema(description = "局限性") String limitations,
            @Schema(description = "提示模式") String promptMode,
            @Schema(description = "是否降级") Boolean degraded,
            @Schema(description = "忽略的字段") String ignoredFields,
            @Schema(description = "媒体理解") String mediaUnderstanding,
            @Schema(description = "最终差异") String finalDiff
    ) {}
}
