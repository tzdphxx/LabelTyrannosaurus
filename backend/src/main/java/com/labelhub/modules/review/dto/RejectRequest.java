package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "审核驳回请求")
public record RejectRequest(
        @NotBlank @Schema(description = "驳回原因") String reason,
        @Min(1) @Schema(description = "审核级别") int reviewLevel
) {
}
