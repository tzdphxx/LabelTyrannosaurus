package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "审核人仪表盘响应")
public record ReviewerDashboardResponse(
        @Schema(description = "待审核数量") int pendingCount,
        @Schema(description = "今日已审核数量") int todayReviewedCount,
        @Schema(description = "累计通过数量") int totalApproved,
        @Schema(description = "累计驳回数量") int totalRejected,
        @Schema(description = "通过率") BigDecimal approvalRate
) {
}
