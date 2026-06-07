package com.labelhub.modules.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.labelhub.modules.reward.dto.RewardRuleRequest;
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
        @Schema(description = "任务配额（FCFS/QUOTA_GRAB必填，ASSIGNED自动推导）", example = "100")
        @Min(0)
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
        @Schema(description = "AI 审核配置 ID。创建新任务时不建议传该字段；当前创建接口要求内联提供 aiProviderId、aiPrompt、aiScoringDimensions，创建成功后后端会自动回写配置 ID", example = "30")
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
        @Schema(description = """
                AI 审核策略:
                LIGHTWEIGHT — 单路 LLM（默认，兼容存量）
                PARALLEL_VOTE — 多模型并行投票（系统自动用当前模型复制满足票数）
                DEEP_DIMENSION — 维度专项模型 + 维度内投票
                AGENT_DEBATE — 多 Agent 辩论""",
                example = "LIGHTWEIGHT") String aiReviewStrategy,
        @Schema(description = """
                AI 流转策略（内联创建 AI 配置时可选，仅在内联创建时生效）:
                MANUAL_FIRST — AI 只提建议，结果一律转人工（默认）
                AI_PASS_ONLY — 允许 AI 直接过审，打回仍转人工
                AI_REJECT_ONLY — 允许 AI 直接打回，通过仍转人工
                AI_PASS_AND_REJECT — 允许 AI 直接过审与直接打回
                ALWAYS_MANUAL — 始终转人工""",
                example = "MANUAL_FIRST") String aiFlowPolicy,
        @Schema(description = "领取策略", example = "FCFS", allowableValues = {"FCFS", "QUOTA_GRAB", "ASSIGNED"})
        String strategy,
        @Schema(description = "单人并发未完成上限（仅 QUOTA_GRAB 有效）", example = "10")
        @Min(1)
        Integer maxClaimsPerLabeler,
        @Schema(description = "审核级别数（1=单级审核，2=初审+终审，3=初审+复审+终审）", example = "3")
        @Min(1)
        Integer reviewLevelCount,
        @Schema(description = "通过 /api/v1/files/upload 上传的数据集文件 ID", example = "99")
        Long datasetFileId,
        @Schema(description = "任务奖励规则，可选，填写后自动创建初始奖励规则")
        @Valid
        RewardRuleRequest rewardRule,
        @Schema(description = "ASSIGNED 策略指定的标注员 ID；发布时自动领取全部题目", example = "20")
        Long assignedLabelerId
) {
}
