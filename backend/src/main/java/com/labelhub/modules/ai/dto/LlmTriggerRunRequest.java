package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Schema(description = "大模型触发运行请求")
public record LlmTriggerRunRequest(
        @Schema(description = "任务ID")
        @NotNull Long taskId,
        @Schema(description = "模板版本ID")
        @NotNull Long templateVersionId,
        @Schema(description = "组件ID")
        @NotBlank String componentId,
        @Schema(description = "数据集项ID")
        Long datasetItemId,
        @Schema(description = "分配ID")
        Long assignmentId,
        @Schema(description = "当前答案JSON")
        Map<String, Object> currentAnswerJson,
        @Schema(description = "是否为预览模式")
        boolean previewMode
) {
}
