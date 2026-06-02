package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "批量审核通过请求")
public record BatchApproveRequest(
        @NotEmpty @Schema(description = "提交ID列表") List<Long> submissionIds,
        @Schema(description = "审核评论") String reviewComment,
        @Schema(description = "审核级别") int reviewLevel
) {
}
