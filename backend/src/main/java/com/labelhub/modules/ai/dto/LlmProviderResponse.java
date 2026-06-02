package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "大模型供应商响应")
public record LlmProviderResponse(
        @Schema(description = "供应商ID")
        Long id,
        @Schema(description = "供应商编码")
        String providerCode,
        @Schema(description = "供应商名称")
        String providerName,
        @Schema(description = "基础URL")
        String baseUrl,
        @Schema(description = "默认模型")
        String defaultModel,
        @Schema(description = "自定义请求头")
        Map<String, String> customHeaders,
        @Schema(description = "是否启用")
        Boolean enabled,
        @Schema(description = "平台级别每分钟速率限制")
        Integer platformRateLimitPerMinute,
        @Schema(description = "任务级别每分钟速率限制")
        Integer taskRateLimitPerMinute,
        @Schema(description = "用户级别每分钟速率限制")
        Integer userRateLimitPerMinute,
        @Schema(description = "是否支持视觉识别")
        Boolean supportVision,
        @Schema(description = "是否支持多图识别")
        Boolean supportMultiImage,
        @Schema(description = "最大图片数量")
        Integer maxImageCount,
        @Schema(description = "视觉模型名称")
        String visionModel,
        @Schema(description = "结构化输出模式")
        String structuredOutputMode,
        @Schema(description = "API密钥是否已配置")
        Boolean apiKeyConfigured,
        @Schema(description = "所有者用户ID")
        Long ownerId,
        @Schema(description = "创建者用户ID")
        Long createdBy,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}
