package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "批量分配审核人请求")
public record BatchAssignRequest(
        @NotEmpty @Schema(description = "提交ID列表") List<Long> submissionIds,
        @NotNull @Schema(description = "目标审核人ID") Long targetReviewerId
) {
}
