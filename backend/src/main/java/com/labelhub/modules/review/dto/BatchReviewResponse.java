package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "批量审核响应")
public record BatchReviewResponse(
        @Schema(description = "总数") int total,
        @Schema(description = "成功数量") int successCount,
        @Schema(description = "失败数量") int failCount,
        @Schema(description = "各条目结果列表") List<BatchReviewItemResult> results
) {
}
