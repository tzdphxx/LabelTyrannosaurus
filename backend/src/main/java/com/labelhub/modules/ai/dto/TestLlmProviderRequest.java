package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Schema(description = "测试大模型供应商请求")
public record TestLlmProviderRequest(
        @Schema(description = "API密钥", example = "sk-...")
        @Size(max = 4096) String apiKey,
        @Schema(description = "模型名称", example = "gpt-4o")
        @Size(max = 128) String modelName,
        @Schema(description = "自定义请求头")
        Map<String, String> customHeaders
) {
}
