package com.labelhub.modules.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "每日贡献积分")
public record DailyContributionPoint(
        @Schema(description = "统计日期") LocalDate statDate,
        @Schema(description = "提交数量") Integer submittedCount,
        @Schema(description = "通过数量") Integer approvedCount,
        @Schema(description = "驳回数量") Integer rejectedCount,
        @Schema(description = "奖励金额") BigDecimal rewardAmount) {
}
