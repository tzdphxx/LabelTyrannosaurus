package com.labelhub.modules.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "更新草稿任务请求")
public record UpdateTaskRequest(
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
        @Schema(description = "每条数据需要的标注份数，当前固定为 1", example = "1")
        @NotNull
        @Min(1)
        @Max(1)
        Integer overlapCount,
        @Schema(description = "已发布模板版本 ID", example = "20")
        Long publishedTemplateVersionId,
        @Schema(description = "AI 审核配置 ID", example = "30")
        Long aiReviewConfigId,
        @Schema(description = "审核级别数（1=单级审核，2=初审+终审，3=初审+复审+终审）", example = "3")
        @Min(1)
        Integer reviewLevelCount
) {
}
