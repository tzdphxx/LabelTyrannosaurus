package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Owner模型选项响应（仅展示选择和必要字段，不包含API密钥等敏感信息）")
public record OwnerModelOptionResponse(
        @Schema(description = "供应商ID")
        Long id,
        @Schema(description = "供应商编码")
        String providerCode,
        @Schema(description = "供应商名称")
        String providerName,
        @Schema(description = "默认模型")
        String defaultModel,
        @Schema(description = "是否支持视觉识别")
        Boolean supportVision,
        @Schema(description = "是否支持多图识别")
        Boolean supportMultiImage,
        @Schema(description = "最大图片数量")
        Integer maxImageCount,
        @Schema(description = "视觉模型名称")
        String visionModel,
        @Schema(description = "结构化输出模式")
        String structuredOutputMode
) {
}
