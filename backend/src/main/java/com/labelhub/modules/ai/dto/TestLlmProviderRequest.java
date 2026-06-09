package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Schema(description = "测试 LLM Provider 连通性请求")
public record TestLlmProviderRequest(
        @Schema(description = "临时测试 API Key；未传则使用已保存密钥，临时值不会保存", example = "sk-***")
        @Size(max = 4096) String apiKey,
        @Schema(description = "临时测试模型名；未传则使用 Provider defaultModel", example = "qwen-plus")
        @Size(max = 128) String modelName,
        @Schema(description = "临时附加请求头，会与已保存 Header 合并，临时值不会保存")
        Map<String, String> customHeaders
) {
}
