package com.labelhub.modules.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "指派记录响应")
public record DispatchEntryResponse(
        @Schema(description = "指派记录 ID", example = "1")
        Long dispatchId,
        @Schema(description = "任务 ID", example = "10")
        Long taskId,
        @Schema(description = "数据集项 ID", example = "500")
        Long datasetItemId,
        @Schema(description = "标注员用户 ID", example = "100")
        Long labelerId,
        @Schema(description = "指派状态：PENDING（待领取）/ CLAIMED（已领取）/ EXPIRED（已过期）/ REVOKED（已撤销）")
        String status,
        @Schema(description = "指派时间")
        LocalDateTime dispatchedAt,
        @Schema(description = "领取时间，未领取时为 null")
        LocalDateTime claimedAt
) {
}
