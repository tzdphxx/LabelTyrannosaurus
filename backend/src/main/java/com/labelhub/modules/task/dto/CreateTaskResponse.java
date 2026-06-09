package com.labelhub.modules.task.dto;

import com.labelhub.modules.dataset.dto.DatasetImportJobResponse;
import com.labelhub.modules.reward.dto.RewardRuleResponse;
import com.labelhub.modules.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建任务响应")
public record CreateTaskResponse(
        @Schema(description = "任务 ID", example = "100")
        Long taskId,
        @Schema(description = "任务状态", example = "DRAFT")
        TaskStatus status,
        @Schema(description = "随任务创建的导入作业（未传 datasetFileId 时为 null）")
        DatasetImportJobResponse datasetImportJob,
        @Schema(description = "随任务创建的奖励规则（未传 rewardRule 时为 null）")
        RewardRuleResponse rewardRule
) {
}
