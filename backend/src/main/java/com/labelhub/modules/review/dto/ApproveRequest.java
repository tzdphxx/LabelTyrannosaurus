package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "审核通过请求")
public record ApproveRequest(
        @Schema(description = "审核评论") String reviewComment,
        @Min(1) @Schema(description = "审核级别") int reviewLevel
) {
}
