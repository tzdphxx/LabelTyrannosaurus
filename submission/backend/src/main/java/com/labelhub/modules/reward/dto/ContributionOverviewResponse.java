package com.labelhub.modules.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "标注员贡献概览")
public record ContributionOverviewResponse(
        @Schema(description = "标注员用户 ID", example = "100")
        Long labelerId,
        @Schema(description = "已领取数量")
        Integer claimedCount,
        @Schema(description = "已提交数量（不含待审核）")
        Integer submittedCount,
        @Schema(description = "待审核数量")
        Integer pendingReviewCount,
        @Schema(description = "已通过数量")
        Integer approvedCount,
        @Schema(description = "已驳回数量")
        Integer rejectedCount,
        @Schema(description = "累计获得奖励")
        BigDecimal totalReward,
        @Schema(description = "通过率")
        BigDecimal approvalRate) {
}
