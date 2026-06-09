package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Schema(description = "创建 LLM Provider 请求")
public record CreateLlmProviderRequest(
        @Schema(description = "Provider 编码，建议使用小写字母、数字、短横线或下划线", example = "dashscope")
        @NotBlank @Size(max = 64) String providerCode,
        @Schema(description = "Provider 展示名称", example = "DashScope Qwen Plus")
        @NotBlank @Size(max = 100) String providerName,
        @Schema(description = "OpenAI-compatible Base URL；服务端会去掉末尾斜杠", example = "https://dashscope.aliyuncs.com/compatible-mode/v1")
        @NotBlank @Size(max = 500) String baseUrl,
        @Schema(description = "管理员配置的 API Key；服务端 AES-GCM 加密保存，响应中不会返回", example = "sk-***")
        @NotBlank @Size(max = 4096) String apiKey,
        @Schema(description = "默认模型名；Owner 缺省 modelName 时使用该模型", example = "qwen-plus")
        @NotBlank @Size(max = 128) String defaultModel,
        @Schema(description = "额外请求头；空 key/value 会被忽略，敏感 Header 响应时会被屏蔽")
        Map<String, String> customHeaders,
        @Schema(description = "平台级每分钟限流，0 或 null 表示不限制", example = "100")
        @Min(0) Integer platformRateLimitPerMinute,
        @Schema(description = "任务级每分钟限流，0 或 null 表示不限制", example = "50")
        @Min(0) Integer taskRateLimitPerMinute,
        @Schema(description = "用户级每分钟限流，0 或 null 表示不限制", example = "20")
        @Min(0) Integer userRateLimitPerMinute,
        @Schema(description = "是否支持视觉输入；未传按 false 处理", example = "false")
        Boolean supportVision,
        @Schema(description = "是否支持单次请求多图输入；未传按 false 处理", example = "false")
        Boolean supportMultiImage,
        @Schema(description = "单次请求最大图片数；未传按 10 处理", example = "10")
        @Min(0) Integer maxImageCount,
        @Schema(description = "视觉模型名；为空时视觉请求使用 defaultModel", example = "qwen-vl-plus")
        @Size(max = 100) String visionModel,
        @Schema(description = "结构化输出模式：NONE、JSON_OBJECT、JSON_SCHEMA；未传或非法值保存为空，运行时按不强制结构化输出处理", example = "JSON_OBJECT")
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
