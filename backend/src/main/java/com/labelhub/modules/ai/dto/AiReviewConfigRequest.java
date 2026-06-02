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

@Schema(description = "AI审核配置请求")
public record AiReviewConfigRequest(
        @Schema(description = "供应商ID")
        @NotNull Long providerId,
        @Schema(description = "模型名称", example = "gpt-4o")
        @NotBlank @Size(max = 128) String modelName,
        @Schema(description = "提示词模板")
        @NotBlank @Size(max = 10000) String promptTemplate,
        @Schema(description = "评分维度列表")
        @NotEmpty List<@NotBlank @Size(max = 64) String> scoringDimensions,
        @Schema(description = "通过阈值（0-100）", example = "80.00")
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal passThreshold,
        @Schema(description = "人工审核阈值（0-100）", example = "60.00")
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal manualReviewThreshold,
        @Schema(description = "输出结构定义")
        @NotEmpty Map<String, Object> outputSchema,
        @Schema(description = "最大重试次数", example = "3")
        @Min(0) @Max(10) Integer maxRetry,
        @Schema(description = "AI流程策略")
        String aiFlowPolicy,
        @Schema(description = "是否允许AI直接通过")
        Boolean allowAiDirectApprove,
        @Schema(description = "是否允许AI直接拒绝")
        Boolean allowAiDirectReject,
        @Schema(description = "拒绝阈值（0-100）", example = "40.00")
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal rejectThreshold,
        @Schema(description = "置信度阈值（0-1）", example = "0.80")
        @DecimalMin("0.00") @DecimalMax("1.00") BigDecimal confidenceThreshold,
        @Schema(description = "强制人工审核的风险标记列表")
        List<String> riskFlagsForceManual,
        @Schema(description = "是否启用多模态")
        Boolean multimodalEnabled,
        @Schema(description = "降级惩罚系数（0-1）", example = "0.20")
        @DecimalMin("0.00") @DecimalMax("1.00") BigDecimal degradationPenalty,
        @Schema(description = "视觉识别精度", example = "auto")
        @Size(max = 20) String visionDetail,
        @Schema(description = "每次请求最大图片数量", example = "5")
        @Min(0) @Max(20) Integer maxImagesPerRequest,
        @Schema(description = "降级时是否允许AI直接通过")
        Boolean allowAiDirectApproveWhenDegraded
) {
    public AiReviewConfigRequest(Long providerId, String modelName, String promptTemplate,
                                 List<String> scoringDimensions, BigDecimal passThreshold,
                                 BigDecimal manualReviewThreshold, Map<String, Object> outputSchema,
                                 Integer maxRetry, String aiFlowPolicy, Boolean allowAiDirectApprove,
                                 Boolean allowAiDirectReject, BigDecimal rejectThreshold,
                                 BigDecimal confidenceThreshold, List<String> riskFlagsForceManual) {
        this(providerId, modelName, promptTemplate, scoringDimensions, passThreshold, manualReviewThreshold,
                outputSchema, maxRetry, aiFlowPolicy, allowAiDirectApprove, allowAiDirectReject,
                rejectThreshold, confidenceThreshold, riskFlagsForceManual,
                true, new BigDecimal("0.20"), "auto", 5, false);
    }
}
