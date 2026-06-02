package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.modules.review.domain.ConflictStatus;

@Schema(description = "冲突解决响应")
public record ConflictResolveResponse(
        @Schema(description = "冲突组ID") Long groupId,
        @Schema(description = "冲突状态") ConflictStatus status,
        @Schema(description = "黄金标准提交ID") Long goldenSubmissionId,
        @Schema(description = "审核记录ID") Long reviewRecordId
) {
}
