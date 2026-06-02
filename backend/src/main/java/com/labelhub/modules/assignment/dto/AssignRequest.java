package com.labelhub.modules.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Owner 指派题目请求")
public record AssignRequest(
        @Schema(description = "目标标注员 ID")
        @NotNull
        Long labelerId,
        @Schema(description = "要指派的题目 ID 列表")
        @NotEmpty
        List<Long> datasetItemIds
) {
}
