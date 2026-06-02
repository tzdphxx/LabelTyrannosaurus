package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "AI审核配置响应")
public record AiReviewConfigResponse(
        @Schema(description = "配置ID")
        Long id,
        @Schema(description = "任务ID")
        Long taskId,
        @Schema(description = "供应商ID")
        Long providerId,
        @Schema(description = "模型名称")
        String modelName,
        @Schema(description = "提示词模板")
        String promptTemplate,
        @Schema(description = "评分维度列表")
        List<String> scoringDimensions,
        @Schema(description = "通过阈值（0-100）")
        BigDecimal passThreshold,
        @Schema(description = "人工审核阈值（0-100）")
        BigDecimal manualReviewThreshold,
        @Schema(description = "输出结构定义")
        Map<String, Object> outputSchema,
        @Schema(description = "提示词版本")
        String promptVersion,
        @Schema(description = "最大重试次数")
        Integer maxRetry,
        @Schema(description = "AI流程策略")
        String aiFlowPolicy,
        @Schema(description = "是否允许AI直接通过")
        Boolean allowAiDirectApprove,
        @Schema(description = "是否允许AI直接拒绝")
        Boolean allowAiDirectReject,
        @Schema(description = "拒绝阈值（0-100）")
        BigDecimal rejectThreshold,
        @Schema(description = "置信度阈值（0-1）")
        BigDecimal confidenceThreshold,
        @Schema(description = "强制人工审核的风险标记列表")
        List<String> riskFlagsForceManual,
        @Schema(description = "是否启用多模态")
        Boolean multimodalEnabled,
        @Schema(description = "降级惩罚系数（0-1）")
        BigDecimal degradationPenalty,
        @Schema(description = "视觉识别精度")
        String visionDetail,
        @Schema(description = "每次请求最大图片数量")
        Integer maxImagesPerRequest,
        @Schema(description = "降级时是否允许AI直接通过")
        Boolean allowAiDirectApproveWhenDegraded
) {
    public AiReviewConfigResponse(Long id, Long taskId, Long providerId, String modelName,
                                  String promptTemplate, List<String> scoringDimensions,
                                  BigDecimal passThreshold, BigDecimal manualReviewThreshold,
                                  Map<String, Object> outputSchema, String promptVersion, Integer maxRetry,
                                  String aiFlowPolicy, Boolean allowAiDirectApprove, Boolean allowAiDirectReject,
                                  BigDecimal rejectThreshold, BigDecimal confidenceThreshold,
                                  List<String> riskFlagsForceManual) {
        this(id, taskId, providerId, modelName, promptTemplate, scoringDimensions, passThreshold,
                manualReviewThreshold, outputSchema, promptVersion, maxRetry, aiFlowPolicy,
                allowAiDirectApprove, allowAiDirectReject, rejectThreshold, confidenceThreshold,
                riskFlagsForceManual, true, new BigDecimal("0.20"), "auto", 5, false);
    }
}
