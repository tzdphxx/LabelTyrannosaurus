package com.labelhub.modules.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "任务标注员响应")
public record TaskLabelerResponse(
        @Schema(description = "标注员 ID", example = "10")
        Long labelerId,
        @Schema(description = "用户名", example = "labeler")
        String username,
        @Schema(description = "显示名称", example = "张三")
        String displayName,
        @Schema(description = "已领取数量", example = "30")
        int claimedCount,
        @Schema(description = "已提交数量", example = "25")
        int submittedCount,
        @Schema(description = "已通过数量", example = "20")
        int approvedCount,
        @Schema(description = "已驳回数量", example = "5")
        int rejectedCount,
        @Schema(description = "已取消数量", example = "0")
        int cancelledCount,
        @Schema(description = "首次领取时间", example = "2026-05-01T10:00:00")
        LocalDateTime firstClaimedAt,
        @Schema(description = "最近活动时间", example = "2026-05-15T14:30:00")
        LocalDateTime lastActivityAt
) {
}
