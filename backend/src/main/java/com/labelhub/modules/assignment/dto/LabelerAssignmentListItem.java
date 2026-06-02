package com.labelhub.modules.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import java.time.LocalDateTime;

@Schema(description = "标注人任务列表项")
public record LabelerAssignmentListItem(
        @Schema(description = "分配ID") Long assignmentId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "任务标题") String taskTitle,
        @Schema(description = "数据集条目ID") Long datasetItemId,
        @Schema(description = "分配状态") AssignmentStatus status,
        @Schema(description = "草稿版本") Integer draftVersion,
        @Schema(description = "认领时间") LocalDateTime claimedAt,
        @Schema(description = "退回时间") LocalDateTime returnedAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
}
