package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "审核认领响应")
public record ReviewClaimResponse(
        @Schema(description = "已认领的提交ID列表") List<Long> claimedSubmissionIds,
        @Schema(description = "已认领数量") int claimedCount
) {
}
