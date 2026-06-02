package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "冲突解决请求")
public record ConflictResolveRequest(
        @NotNull @Schema(description = "黄金标准提交ID") Long goldenSubmissionId,
        @NotBlank @Schema(description = "解决原因") String reason
) {
}
