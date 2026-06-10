package com.labelhub.modules.admin.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "管理端看板 KPI 指标")
public record AdminDashboardKpis(
        @Schema(description = "活跃任务数，包含当前已发布任务以及周期内产生领取或提交的任务", example = "12")
        long activeTaskCount,
        @Schema(description = "周期内标注员领取次数", example = "340")
        long claimedCount,
        @Schema(description = "周期内有效提交数，不包含 SUPERSEDED 提交", example = "286")
        long submittedCount,
        @Schema(description = "当前待终审提交数", example = "31")
        long pendingReviewCount,
        @Schema(description = "周期内审核通过率，approved / (approved + rejected)，分母为 0 时返回 0", example = "0.8200")
        BigDecimal approvalRate,
        @Schema(description = "周期内审核打回率，rejected / (approved + rejected)，分母为 0 时返回 0", example = "0.1800")
        BigDecimal rejectionRate,
        @Schema(description = "周期内正向奖励支出汇总金额", example = "1280.50")
        BigDecimal rewardAmount
) {
}
