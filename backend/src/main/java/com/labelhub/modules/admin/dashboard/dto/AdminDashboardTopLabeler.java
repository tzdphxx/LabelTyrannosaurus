package com.labelhub.modules.admin.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "管理端看板标注员排行榜条目")
public record AdminDashboardTopLabeler(
        @Schema(description = "标注员用户 ID", example = "20")
        Long labelerId,
        @Schema(description = "标注员展示名称，优先 displayName，缺失时使用 username", example = "labeler-a")
        String displayName,
        @Schema(description = "周期内该标注员有效提交数", example = "46")
        long submittedCount,
        @Schema(description = "周期内该标注员审核通过数", example = "39")
        long approvedCount,
        @Schema(description = "周期内该标注员获得的正向奖励金额", example = "210.00")
        BigDecimal rewardAmount
) {
}
