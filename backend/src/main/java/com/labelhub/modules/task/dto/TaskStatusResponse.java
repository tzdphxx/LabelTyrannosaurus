package com.labelhub.modules.task.dto;

import com.labelhub.modules.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "任务状态变更响应")
public record TaskStatusResponse(
        @Schema(description = "任务 ID", example = "100")
        Long taskId,
        @Schema(description = "任务状态", example = "DRAFT")
        TaskStatus status
) {}
