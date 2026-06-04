package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "AI 审核配置请求")
public record AiReviewConfigRequest(
        @Schema(description = "LLM 供应商 ID", example = "1") @NotNull Long providerId,
        @Schema(description = "模型名称", example = "qwen-plus") @NotBlank @Size(max = 128) String modelName,
        @Schema(description = "审核 Prompt 模板（标注规则说明）", example = "请评估标注结果的准确性和完整性")
        @NotBlank @Size(max = 10000) String promptTemplate,
        @Schema(description = "评分维度列表", example = "[\"准确性\",\"完整性\",\"安全性\"]")
        @NotEmpty List<@NotBlank @Size(max = 64) String> scoringDimensions,
        @Schema(description = "通过阈值（0-100）", example = "80.00")
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal passThreshold,
        @Schema(description = "人工复核阈值（0-100，低于此值打回）", example = "60.00")
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal manualReviewThreshold,
        @Schema(description = "最大重试次数（0-10）", example = "3") @Min(0) @Max(10) Integer maxRetry,
        @Schema(description = "AI 流转策略: MANUAL_FIRST | AI_PASS_ONLY | AI_REJECT_ONLY | AI_PASS_AND_REJECT | ALWAYS_MANUAL",
                example = "MANUAL_FIRST") String aiFlowPolicy,
        @Schema(description = "是否允许 AI 直接通过") Boolean allowAiDirectApprove,
        @Schema(description = "是否允许 AI 直接打回") Boolean allowAiDirectReject,
        @Schema(description = "打回阈值（0-100）", example = "40.00")
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal rejectThreshold,
        @Schema(description = "置信度阈值（0.00-1.00）", example = "0.85")
        @DecimalMin("0.00") @DecimalMax("1.00") BigDecimal confidenceThreshold,
        @Schema(description = "强制转人工的风险标记列表") List<String> riskFlagsForceManual,
        @Schema(description = "是否启用多模态（图片/视频输入）", example = "true") Boolean multimodalEnabled,
        @Schema(description = "多模态降级惩罚系数（0.00-1.00）", example = "0.20")
        @DecimalMin("0.00") @DecimalMax("1.00") BigDecimal degradationPenalty,
        @Schema(description = "视觉精度: auto | low | high", example = "auto") @Size(max = 20) String visionDetail,
        @Schema(description = "单次请求最大图片数（0-20）", example = "5") @Min(0) @Max(20) Integer maxImagesPerRequest,
        @Schema(description = "降级时是否仍允许 AI 直接通过") Boolean allowAiDirectApproveWhenDegraded,

        @Schema(description = "审核策略: LIGHTWEIGHT(单路,默认) | PARALLEL_VOTE(多模型投票) | DEEP_DIMENSION(维度专项) | AGENT_DEBATE(辩论)",
                example = "LIGHTWEIGHT") String reviewStrategy,
        @Schema(description = "投票模型列表, JSON[{providerId,modelName}]; 仅1个时自动复制满足最低票数",
                example = "[{\"providerId\":1,\"modelName\":\"qwen-plus\"}]") List<Map<String, Object>> voteModels,
        @Schema(description = "最少一致票数(1-10), 默认2", example = "2") @Min(1) @Max(10) Integer voteMinAgreement,
        @Schema(description = "深度模式维度→模型映射, JSON{dim:[{providerId,modelName}]}") Map<String, List<Map<String, Object>>> dimensionReviewers
) {
    public AiReviewConfigRequest(Long providerId, String modelName, String promptTemplate,
                                 List<String> scoringDimensions, BigDecimal passThreshold,
                                 BigDecimal manualReviewThreshold,
                                 Integer maxRetry, String aiFlowPolicy, Boolean allowAiDirectApprove,
                                 Boolean allowAiDirectReject, BigDecimal rejectThreshold,
                                 BigDecimal confidenceThreshold, List<String> riskFlagsForceManual) {
        this(providerId, modelName, promptTemplate, scoringDimensions, passThreshold, manualReviewThreshold,
                maxRetry, aiFlowPolicy, allowAiDirectApprove, allowAiDirectReject,
                rejectThreshold, confidenceThreshold, riskFlagsForceManual,
                true, new BigDecimal("0.20"), "auto", 5, false,
                null, null, null, null);
    }
}
