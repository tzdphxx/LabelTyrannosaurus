package com.labelhub.modules.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "批量领取请求")
public record BatchClaimRequest(
        @Schema(description = "领取数量", example = "5")
        @Min(1) @Max(50)
        int count
) {
}
