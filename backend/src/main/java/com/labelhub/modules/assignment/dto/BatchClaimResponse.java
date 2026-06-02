package com.labelhub.modules.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "批量领取响应")
public record BatchClaimResponse(
        @Schema(description = "成功领取的 assignment 列表")
        List<AssignmentClaimResponse> assignments,
        @Schema(description = "成功领取数量")
        int claimedCount,
        @Schema(description = "失败数量（无库存/配额满等）")
        int failedCount
) {
}
