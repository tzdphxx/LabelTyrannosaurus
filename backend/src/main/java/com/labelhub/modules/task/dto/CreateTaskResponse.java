package com.labelhub.modules.task.dto;

import com.labelhub.modules.dataset.dto.DatasetImportJobResponse;
import com.labelhub.modules.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建任务响应")
public record CreateTaskResponse(
        @Schema(description = "任务 ID", example = "100")
        Long taskId,
        @Schema(description = "任务状态", example = "DRAFT")
        TaskStatus status,
        @Schema(description = "随任务创建的数据集导入任务；未上传数据集文件时为 null")
        DatasetImportJobResponse datasetImportJob
) {
}
