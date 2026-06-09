package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@Schema(description = "LLM 字段触发请求。标注员点击组件后后端构建 LLM 上下文并发起调用。")
public record LlmTriggerRunRequest(
        @Schema(description = "旧版厂商 ID（标注员触发时忽略，保留用于旧客户端兼容）")
        Long providerId,
        @Schema(description = "旧版模型名称（标注员触发时忽略，保留用于旧客户端兼容）")
        @Size(max = 128) String modelName,
        @Schema(description = "旧版 Prompt 模板（标注员触发时忽略，保留用于旧客户端兼容）")
        @Size(max = 10000) String promptTemplate,
        @Schema(description = "旧版目标字段（标注员触发时忽略，保留用于旧客户端兼容）")
        List<@Size(max = 64) String> targetFields,
        @Schema(description = "题目 ID，Owner 预览测试时指定要测试的题目")
        Long datasetItemId,
        @Schema(description = "模板 ID（整型，仅回显不校验；整模板模式下不再用于定位单个组件）", example = "4502")
        Long componentId,
        @Schema(description = "当前草稿答案 JSON")
        Map<String, Object> currentAnswerJson,
        @Schema(description = "标注员或 Owner 的额外补充指令")
        @Size(max = 1000) String userInstruction
) {
}
