package com.labelhub.modules.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "任务审核员响应")
public record TaskReviewerResponse(
        @Schema(description = "审核员 ID", example = "20")
        Long reviewerId,
        @Schema(description = "用户名", example = "reviewer")
        String username,
        @Schema(description = "显示名称", example = "李四")
        String displayName,
        @Schema(description = "指派时间", example = "2026-05-01T10:00:00")
        LocalDateTime assignedAt
) {
}
