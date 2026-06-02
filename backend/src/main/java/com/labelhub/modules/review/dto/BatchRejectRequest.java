package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "批量审核驳回请求")
public record BatchRejectRequest(
        @NotEmpty @Schema(description = "提交ID列表") List<Long> submissionIds,
        @NotBlank @Schema(description = "驳回原因") String reason,
        @Schema(description = "审核级别") int reviewLevel
) {
}
