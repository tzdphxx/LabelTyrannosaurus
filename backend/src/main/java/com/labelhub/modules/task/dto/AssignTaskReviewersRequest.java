package com.labelhub.modules.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "指派任务审核员请求")
public record AssignTaskReviewersRequest(
        @Schema(description = "审核员 ID 列表", example = "[101, 102, 103]")
        @NotEmpty List<Long> reviewerIds
) {
}
