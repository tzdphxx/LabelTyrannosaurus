package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.modules.ai.domain.AiDecision;

@Schema(description = "导出黄金标准条目")
public record ExportGoldenItem(
        @Schema(description = "提交ID") Long submissionId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "数据集条目ID") Long datasetItemId,
        @Schema(description = "条目JSON引用") String itemJsonRef,
        @Schema(description = "标注人ID") Long labelerId,
        @Schema(description = "版本号") Integer versionNo,
        @Schema(description = "答案JSON") String answerJson,
        @Schema(description = "AI决策") AiDecision aiDecision,
        @Schema(description = "AI摘要") String aiSummary,
        @Schema(description = "审核摘要") String reviewSummary,
        @Schema(description = "审计引用") Long auditRef
) {
}
