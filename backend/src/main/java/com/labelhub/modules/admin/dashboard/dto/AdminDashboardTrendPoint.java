package com.labelhub.modules.admin.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理端看板单日趋势点")
public record AdminDashboardTrendPoint(
        @Schema(description = "自然日日期", example = "2026-06-01")
        LocalDate date,
        @Schema(description = "该自然日有效提交数", example = "42")
        long submittedCount,
        @Schema(description = "该自然日审核通过数", example = "35")
        long approvedCount,
        @Schema(description = "该自然日审核打回数", example = "7")
        long rejectedCount,
        @Schema(description = "该自然日正向奖励支出金额", example = "188.00")
        BigDecimal rewardAmount
) {
}
