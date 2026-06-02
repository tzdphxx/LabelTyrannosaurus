package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.modules.ai.domain.AiDecision;
import com.labelhub.modules.submission.domain.SubmissionStatus;

@Schema(description = "提交审核条目")
public record SubmissionReviewItem(
        @Schema(description = "提交ID") Long submissionId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "数据集条目ID") Long datasetItemId,
        @Schema(description = "标注人ID") Long labelerId,
        @Schema(description = "提交状态") SubmissionStatus submissionStatus,
        @Schema(description = "AI决策") AiDecision aiDecision,
        @Schema(description = "冲突状态") String conflictStatus,
        @Schema(description = "审核级别") int reviewLevel
) {
}
