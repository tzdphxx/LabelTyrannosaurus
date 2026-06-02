package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "批量标记为人工审核请求")
public record BatchMarkManualRequest(
        @NotEmpty @Schema(description = "提交ID列表") List<Long> submissionIds
) {
}
