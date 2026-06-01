package com.labelhub.modules.task.dto;

import com.labelhub.modules.dataset.dto.DatasetImportJobResponse;
import com.labelhub.modules.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Create task response")
public record CreateTaskResponse(
        @Schema(description = "Task ID", example = "100")
        Long taskId,
        @Schema(description = "Task status", example = "DRAFT")
        TaskStatus status,
        @Schema(description = "Dataset import job created with this task, or null when no dataset file was provided")
        DatasetImportJobResponse datasetImportJob
) {
}
