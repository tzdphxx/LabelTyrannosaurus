package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "AI审核结果查询条件")
public record AiReviewResultQuery(
        @Schema(description = "任务ID")
        Long taskId,
        @Schema(description = "页码", example = "1")
        Integer page,
        @Schema(description = "每页大小", example = "20")
        Integer pageSize,
        @Schema(description = "审核状态")
        String status,
        @Schema(description = "审核决策（通过/拒绝/人工审核）")
        String decision,
        @Schema(description = "开始时间")
        LocalDateTime startTime,
        @Schema(description = "结束时间")
        LocalDateTime endTime
) {

    public int normalizedPage() {
        return page == null || page < 1 ? 1 : page;
    }

    public int normalizedPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }

    public int offset() {
        return (normalizedPage() - 1) * normalizedPageSize();
    }
}
