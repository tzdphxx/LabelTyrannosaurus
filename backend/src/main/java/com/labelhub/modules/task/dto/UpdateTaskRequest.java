package com.labelhub.modules.task.dto;

import com.labelhub.modules.task.domain.Strategy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
        @Schema(description = "已发布模板版本 ID", example = "20")
        Long publishedTemplateVersionId,
        @Schema(description = "AI 审核配置 ID", example = "30")
        Long aiReviewConfigId,
        @Schema(description = "审核级别数（1=单级审核，2=初审+终审，3=初审+复审+终审）", example = "3")
        @Min(1)
        Integer reviewLevelCount,
        @Schema(description = "分发策略：FCFS（先到先得）/ ASSIGNED（指派）/ QUOTA_CLAIM（配额抢单）", example = "FCFS")
        Strategy strategy,
        @Schema(description = "每条通过奖励积分", example = "10.00")
        BigDecimal rewardPerApproval,
        @Schema(description = "每条驳回扣分", example = "5.00")
        BigDecimal penaltyPerRejection,
        @Schema(description = "额外奖励的通过数阈值", example = "50")
        Integer bonusThreshold,
        @Schema(description = "达标后额外奖励积分", example = "100.00")
        BigDecimal bonusPoints
) {
}
