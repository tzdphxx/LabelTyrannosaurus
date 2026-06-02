package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.modules.submission.domain.SubmissionStatus;

@Schema(description = "审核操作响应")
public record ReviewActionResponse(
        @Schema(description = "提交ID") Long submissionId,
        @Schema(description = "提交状态") SubmissionStatus submissionStatus,
        @Schema(description = "审核记录ID") Long reviewRecordId
) {
}
