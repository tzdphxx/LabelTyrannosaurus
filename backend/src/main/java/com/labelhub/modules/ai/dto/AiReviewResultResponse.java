package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "AI审核结果响应")
public record AiReviewResultResponse(
        @Schema(description = "审核结果ID")
        Long id,
        @Schema(description = "提交ID")
        Long submissionId,
        @Schema(description = "代理运行ID")
        Long agentRunId,
        @Schema(description = "供应商ID")
        Long providerId,
        @Schema(description = "模型名称")
        String modelName,
        @Schema(description = "审核状态")
        AiReviewStatus status,
        @Schema(description = "审核决策")
        String decision,
        @Schema(description = "平均分数")
        String averageScore,
        @Schema(description = "各维度评分")
        Map<String, Object> dimensionScores,
        @Schema(description = "风险标记")
        String riskFlags,
        @Schema(description = "审核建议")
        String suggestion,
        @Schema(description = "置信度")
        String confidence,
        @Schema(description = "流程动作")
        String flowAction,
        @Schema(description = "提示词模式")
        String promptMode,
        @Schema(description = "是否降级")
        Boolean degraded,
        @Schema(description = "功能限制列表")
        List<String> limitations,
        @Schema(description = "错误码")
        String errorCode,
        @Schema(description = "错误消息")
        String errorMessage,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
    public AiReviewResultResponse(Long id, Long submissionId, Long agentRunId, Long providerId,
                                  String modelName, AiReviewStatus status, String decision,
                                  String averageScore, Map<String, Object> dimensionScores,
                                  String riskFlags, String suggestion, String confidence,
                                  String flowAction, String errorCode, String errorMessage,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, submissionId, agentRunId, providerId, modelName, status, decision, averageScore,
                dimensionScores, riskFlags, suggestion, confidence, flowAction,
                null, false, List.of(), errorCode, errorMessage, createdAt, updatedAt);
    }
}
