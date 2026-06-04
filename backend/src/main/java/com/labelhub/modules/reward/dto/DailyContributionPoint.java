package com.labelhub.modules.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "每日贡献数据点")
public record DailyContributionPoint(
        @Schema(description = "统计日期")
        LocalDate statDate,
        @Schema(description = "当日提交数")
        Integer submittedCount,
        @Schema(description = "当日通过数")
        Integer approvedCount,
        @Schema(description = "当日驳回数")
        Integer rejectedCount,
        @Schema(description = "当日获得奖励")
        BigDecimal rewardAmount) {
}
