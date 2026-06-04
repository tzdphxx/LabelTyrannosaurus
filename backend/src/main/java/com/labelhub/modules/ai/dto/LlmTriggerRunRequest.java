package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@Schema(description = "LLM 触发器运行请求（前端全量传参，不依赖模板解析）")
public record LlmTriggerRunRequest(
        @Schema(description = "管理员已启用的模型供应商 ID")
        Long providerId,
        @Schema(description = "模型名称")
        @Size(max = 128) String modelName,
        @Schema(description = "提示词模板")
        @Size(max = 10000) String promptTemplate,
        @Schema(description = "AI 输出需要写入的目标字段列表")
        List<@Size(max = 64) String> targetFields,
        @Schema(description = "测试用数据集项 ID（Owner 预览时使用）")
        Long datasetItemId,
        @Schema(description = "Clicked template component id.", example = "summary")
        @Size(max = 128) String componentId,
        @Schema(description = "Current draft answer JSON.")
        Map<String, Object> currentAnswerJson,
        @Schema(description = "Optional extra instruction from the labeler/owner.")
        @Size(max = 1000) String userInstruction
) {
}
