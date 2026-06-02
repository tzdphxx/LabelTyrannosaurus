package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@Schema(description = "LlmTrigger 运行请求（前端全量传参，不依赖模板解析）")
public record LlmTriggerRunRequest(
        @Schema(description = "Admin 启用的模型 ID", example = "50")
        @NotNull Long providerId,
        @Schema(description = "模型名称", example = "qwen-plus")
        @NotBlank @Size(max = 128) String modelName,
        @Schema(description = "提示词模板", example = "根据以下内容生成摘要：{{itemJson}}")
        @NotBlank @Size(max = 10000) String promptTemplate,
        @Schema(description = "AI 输出要填入的目标字段列表", example = "[\"summary\"]")
        @NotEmpty List<@NotBlank @Size(max = 64) String> targetFields,
        @Schema(description = "测试用数据集项 ID（Owner 预览时使用）")
        Long datasetItemId,
        @Schema(description = "当前草稿答案（传入后 AI 可基于已有内容优化）")
        Map<String, Object> currentAnswerJson
) {
}
