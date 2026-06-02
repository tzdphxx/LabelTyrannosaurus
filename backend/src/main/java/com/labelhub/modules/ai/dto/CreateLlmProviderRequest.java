package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Schema(description = "创建大模型供应商请求")
public record CreateLlmProviderRequest(
        @Schema(description = "供应商编码", example = "openai")
        @NotBlank @Size(max = 64) String providerCode,
        @Schema(description = "供应商名称", example = "OpenAI")
        @NotBlank @Size(max = 100) String providerName,
        @Schema(description = "基础URL", example = "https://api.openai.com/v1")
        @NotBlank @Size(max = 500) String baseUrl,
        @Schema(description = "API密钥", example = "sk-...")
        @NotBlank @Size(max = 4096) String apiKey,
        @Schema(description = "默认模型", example = "gpt-4o")
        @NotBlank @Size(max = 128) String defaultModel,
        @Schema(description = "自定义请求头")
        Map<String, String> customHeaders,
        @Schema(description = "平台级别每分钟速率限制", example = "100")
        @Min(0) Integer platformRateLimitPerMinute,
        @Schema(description = "任务级别每分钟速率限制", example = "50")
        @Min(0) Integer taskRateLimitPerMinute,
        @Schema(description = "用户级别每分钟速率限制", example = "30")
        @Min(0) Integer userRateLimitPerMinute,
        @Schema(description = "是否支持视觉识别")
        Boolean supportVision,
        @Schema(description = "是否支持多图识别")
        Boolean supportMultiImage,
        @Schema(description = "最大图片数量", example = "10")
        @Min(0) Integer maxImageCount,
        @Schema(description = "视觉模型名称")
        @Size(max = 100) String visionModel,
        @Schema(description = "结构化输出模式", example = "NONE")
        @Size(max = 20) String structuredOutputMode
) {
    public CreateLlmProviderRequest(String providerCode, String providerName, String baseUrl, String apiKey,
                                    String defaultModel, Map<String, String> customHeaders,
                                    Integer platformRateLimitPerMinute, Integer taskRateLimitPerMinute,
                                    Integer userRateLimitPerMinute) {
        this(providerCode, providerName, baseUrl, apiKey, defaultModel, customHeaders,
                platformRateLimitPerMinute, taskRateLimitPerMinute, userRateLimitPerMinute,
                false, false, 10, null, "NONE");
    }

    public CreateLlmProviderRequest(String providerCode, String providerName, String baseUrl, String apiKey,
                                    String defaultModel, Map<String, String> customHeaders,
                                    Integer platformRateLimitPerMinute, Integer taskRateLimitPerMinute,
                                    Integer userRateLimitPerMinute, Boolean supportVision, Boolean supportMultiImage,
                                    Integer maxImageCount, String visionModel) {
        this(providerCode, providerName, baseUrl, apiKey, defaultModel, customHeaders,
                platformRateLimitPerMinute, taskRateLimitPerMinute, userRateLimitPerMinute,
                supportVision, supportMultiImage, maxImageCount, visionModel, "NONE");
    }
}
