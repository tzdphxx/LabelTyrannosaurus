package com.labelhub.modules.preannotation.dto;

import com.labelhub.modules.preannotation.domain.PreAnnotationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "预标注响应")
public record PreAnnotationResponse(
        @Schema(description = "预标注ID") Long preAnnotationId,
        @Schema(description = "分配ID") Long assignmentId,
        @Schema(description = "Agent运行ID") Long agentRunId,
        @Schema(description = "预标注状态") PreAnnotationStatus status,
        @Schema(description = "建议答案JSON") Map<String, Object> suggestedAnswerJson,
        @Schema(description = "字段建议列表") List<Map<String, Object>> fieldSuggestions,
        @Schema(description = "风险标记列表") List<String> riskFlags,
        @Schema(description = "整体置信度") BigDecimal overallConfidence,
        @Schema(description = "限制列表") List<String> limitations,
        @Schema(description = "提示模式") String promptMode,
        @Schema(description = "是否降级") Boolean degraded,
        @Schema(description = "忽略字段列表") List<String> ignoredFields,
        @Schema(description = "媒体理解结果") Map<String, Object> mediaUnderstanding,
        @Schema(description = "最终差异") Map<String, Object> finalDiff,
        @Schema(description = "错误码") String errorCode,
        @Schema(description = "错误信息") String errorMessage,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
    public PreAnnotationResponse(Long preAnnotationId,
                                 Long assignmentId,
                                 Long agentRunId,
                                 PreAnnotationStatus status,
                                 Map<String, Object> suggestedAnswerJson,
                                 List<Map<String, Object>> fieldSuggestions,
                                 List<String> riskFlags,
                                 BigDecimal overallConfidence,
                                 List<String> limitations,
                                 String promptMode,
                                 Boolean degraded,
                                 Map<String, Object> finalDiff,
                                 String errorCode,
                                 String errorMessage,
                                 LocalDateTime createdAt,
                                 LocalDateTime updatedAt) {
        this(preAnnotationId, assignmentId, agentRunId, status, suggestedAnswerJson, fieldSuggestions,
                riskFlags, overallConfidence, limitations, promptMode, degraded, List.of(), Map.of(),
                finalDiff, errorCode, errorMessage, createdAt, updatedAt);
    }
}
