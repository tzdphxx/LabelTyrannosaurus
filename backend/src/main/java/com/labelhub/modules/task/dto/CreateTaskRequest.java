package com.labelhub.modules.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "创建任务请求")
public record CreateTaskRequest(
        @Schema(description = "任务标题", example = "图像分类标注任务")
        @NotBlank
        @Size(max = 200)
        String title,
        @Schema(description = "任务描述", example = "对商品图片进行类别标注")
        String description,
        @Schema(description = "富文本标注说明")
        String instructionRichText,
        @Schema(description = "任务标签", example = "[\"image\", \"classification\"]")
        List<@Size(max = 64) String> tags,
        @Schema(description = "任务配额", example = "100")
        @NotNull
        @Min(1)
        Integer quota,
        @Schema(description = "截止时间", example = "2026-06-30T23:59:59")
        @NotNull
        @Future
        LocalDateTime deadlineAt,
        @Schema(description = "已发布模板版本 ID", example = "20")
        Long publishedTemplateVersionId,
        @Schema(description = "AI 审核配置 ID（引用已创建的配置，与内联 aiPrompt 互斥）", example = "30")
        Long aiReviewConfigId,
        @Schema(description = "AI 模型供应商 ID（内联创建 AI 配置时必填）")
        Long aiProviderId,
        @Schema(description = "AI 模型名称（可选，如提供则必须匹配 Provider defaultModel）")
        @Size(max = 128)
        String aiModelName,
        @Schema(description = "AI 审核 Prompt 模板（内联创建 AI 配置时必填）")
        @Size(max = 10000)
        String aiPrompt,
        @Schema(description = "AI 评分维度列表（内联创建 AI 配置时必填）")
        List<@Size(max = 64) String> aiScoringDimensions,
        @Schema(description = "AI 通过阈值（0-100）", example = "80.00")
        java.math.BigDecimal aiPassThreshold,
        @Schema(description = "AI 人工复核阈值（0-100）", example = "60.00")
        java.math.BigDecimal aiManualReviewThreshold,
        @Schema(description = "审核级别数（1=单级审核，2=初审+终审，3=初审+复审+终审）", example = "3")
        @Min(1)
        Integer reviewLevelCount,
        @Schema(description = "通过 /api/v1/files/upload 上传的数据集文件 ID", example = "99")
        Long datasetFileId
) {
}
