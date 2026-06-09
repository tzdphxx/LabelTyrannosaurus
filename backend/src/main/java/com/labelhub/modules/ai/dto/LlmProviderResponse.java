package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "LLM Provider 响应。不会返回 API Key 明文或密文；customHeaders 中的敏感值会被服务端屏蔽。")
public record LlmProviderResponse(
        @Schema(description = "Provider ID，任务 AI 配置中使用该值作为 providerId", example = "30")
        Long id,
        @Schema(description = "Provider 编码，用于后台识别供应商或渠道", example = "dashscope")
        String providerCode,
        @Schema(description = "Provider 展示名称，前端模型下拉可展示该值", example = "DashScope Qwen Plus")
        String providerName,
        @Schema(description = "OpenAI-compatible Base URL", example = "https://dashscope.aliyuncs.com/compatible-mode/v1")
        String baseUrl,
        @Schema(description = "默认模型名，也是 Owner 未显式传 modelName 时实际使用的模型", example = "qwen-plus")
        String defaultModel,
        @Schema(description = "已保存的额外请求头；Authorization、Api-Key 等敏感值会显示为 ******")
        Map<String, String> customHeaders,
        @Schema(description = "是否启用；只有启用的 Provider 会出现在 Owner 可选列表", example = "true")
        Boolean enabled,
        @Schema(description = "平台级每分钟限流；null 表示不使用该层限流", example = "100")
        Integer platformRateLimitPerMinute,
        @Schema(description = "任务级每分钟限流；null 表示不使用该层限流", example = "50")
        Integer taskRateLimitPerMinute,
        @Schema(description = "用户级每分钟限流；null 表示不使用该层限流", example = "20")
        Integer userRateLimitPerMinute,
        @Schema(description = "是否支持视觉输入", example = "false")
        Boolean supportVision,
        @Schema(description = "是否支持单次请求多图输入", example = "false")
        Boolean supportMultiImage,
        @Schema(description = "单次请求最大图片数", example = "10")
        Integer maxImageCount,
        @Schema(description = "视觉模型名；为空时视觉请求仍使用 defaultModel", example = "qwen-vl-plus")
        String visionModel,
        @Schema(description = "结构化输出模式：NONE、JSON_OBJECT、JSON_SCHEMA", example = "JSON_OBJECT")
        String structuredOutputMode,
        @Schema(description = "是否已配置 API Key；仅表示配置状态，不是密钥内容", example = "true")
        Boolean apiKeyConfigured,
        @Schema(description = "创建该 Provider 的管理员用户 ID", example = "1")
        Long createdBy,
        @Schema(description = "创建时间", example = "2026-06-06T20:00:00")
        LocalDateTime createdAt,
        @Schema(description = "更新时间", example = "2026-06-06T20:00:00")
        LocalDateTime updatedAt
) {
}
