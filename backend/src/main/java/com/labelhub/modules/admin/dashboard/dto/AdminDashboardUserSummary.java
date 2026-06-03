package com.labelhub.modules.admin.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "管理端看板用户摘要")
public record AdminDashboardUserSummary(
        @Schema(description = "非 SYSTEM 用户总数", example = "126")
        long totalUserCount,
        @Schema(description = "角色人数分布，固定包含 ADMIN、OWNER、LABELER、REVIEWER 四个 key")
        Map<String, Long> roleCounts,
        @Schema(description = "被禁用或禁止登录的非 SYSTEM 用户数", example = "3")
        long disabledUserCount,
        @Schema(description = "统计周期内新增的非 SYSTEM 用户数", example = "8")
        long newUserCount
) {
}
