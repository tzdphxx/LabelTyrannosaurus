package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "AI审核结果分页响应")
public record AiReviewResultPageResponse(
        @Schema(description = "审核结果列表")
        List<AiReviewResultResponse> items,
        @Schema(description = "当前页码")
        int page,
        @Schema(description = "每页大小")
        int pageSize,
        @Schema(description = "总记录数")
        long total
) {
}
