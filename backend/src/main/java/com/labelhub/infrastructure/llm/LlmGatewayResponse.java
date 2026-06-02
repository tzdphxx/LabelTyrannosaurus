package com.labelhub.infrastructure.llm;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "LLM 网关响应")
public record LlmGatewayResponse(
        @Schema(description = "网关调用状态")
        LlmGatewayStatus status,
        @Schema(description = "原始响应文本")
        String rawResponse,
        @Schema(description = "提取的文本内容")
        String contentText,
        @Schema(description = "结构化 JSON 输出")
        Map<String, Object> structuredJson,
        @Schema(description = "调用耗时（毫秒）", example = "1500")
        Long latencyMs,
        @Schema(description = "错误码")
        String errorCode,
        @Schema(description = "错误详情")
        String errorMessage
) {
}
