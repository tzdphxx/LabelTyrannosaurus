package com.labelhub.modules.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "OWNER 可指派标注员选项")
public record AssignableLabelerResponse(
        @Schema(description = "标注员用户 ID", example = "20")
        Long labelerId,
        @Schema(description = "用户名", example = "labeler-a")
        String username,
        @Schema(description = "邮箱", example = "labeler-a@example.com")
        String email,
        @Schema(description = "显示名称", example = "Labeler A")
        String displayName,
        @Schema(description = "头像 URL", example = "https://cdn.example.com/a.png")
        String avatarUrl,
        @Schema(description = "账号是否启用", example = "true")
        Boolean enabled,
        @Schema(description = "是否允许登录", example = "true")
        Boolean loginEnabled,
        @Schema(description = "已领取题目总数量", example = "12")
        Integer claimedCount,
        @Schema(description = "已提交题目总数量", example = "10")
        Integer submittedCount,
        @Schema(description = "待审核题目数量", example = "6")
        Integer pendingReviewCount,
        @Schema(description = "已通过题目数量", example = "3")
        Integer approvedCount,
        @Schema(description = "已驳回题目数量", example = "1")
        Integer rejectedCount,
        @Schema(description = "累计获得奖励", example = "9.00")
        BigDecimal totalReward,
        @Schema(description = "今日提交题目数量", example = "2")
        Integer todaySubmittedCount,
        @Schema(description = "最近提交日期", example = "2026-06-06")
        LocalDate lastSubmitDate,
        @Schema(description = "统计数据更新时间", example = "2026-06-06T10:00:00")
        LocalDateTime statsUpdatedAt,
        @Schema(description = "通过率", example = "0.7500")
        BigDecimal approvalRate
) {
}
