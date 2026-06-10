package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "AI 审核配置响应")
public record AiReviewConfigResponse(
        @Schema(description = "配置 ID") Long id,
        @Schema(description = "任务 ID") Long taskId,
        @Schema(description = "LLM 供应商 ID") Long providerId,
        @Schema(description = "模型名称") String modelName,
        @Schema(description = "审核 Prompt 模板") String promptTemplate,
        @Schema(description = "评分维度列表") List<String> scoringDimensions,
        @Schema(description = "通过阈值") BigDecimal passThreshold,
        @Schema(description = "人工复核阈值") BigDecimal manualReviewThreshold,
        @Schema(description = "输出 JSON Schema") Map<String, Object> outputSchema,
        @Schema(description = "Prompt 版本号") String promptVersion,
        @Schema(description = "最大重试次数") Integer maxRetry,
        @Schema(description = "AI 流转策略") String aiFlowPolicy,
        @Schema(description = "是否允许 AI 直接通过") Boolean allowAiDirectApprove,
        @Schema(description = "是否允许 AI 直接打回") Boolean allowAiDirectReject,
        @Schema(description = "打回阈值") BigDecimal rejectThreshold,
        @Schema(description = "置信度阈值") BigDecimal confidenceThreshold,
        @Schema(description = "强制转人工的风险标记") List<String> riskFlagsForceManual,
        @Schema(description = "是否启用多模态") Boolean multimodalEnabled,
        @Schema(description = "多模态降级惩罚系数") BigDecimal degradationPenalty,
        @Schema(description = "视觉精度") String visionDetail,
        @Schema(description = "最大图片数") Integer maxImagesPerRequest,
        @Schema(description = "降级时是否允许 AI 直接通过") Boolean allowAiDirectApproveWhenDegraded,

        @Schema(description = "审核策略: LIGHTWEIGHT | PARALLEL_VOTE | DEEP_DIMENSION | AGENT_DEBATE") String reviewStrategy,
        @Schema(description = "投票模型列表") List<Map<String, Object>> voteModels,
        @Schema(description = "最少一致票数") Integer voteMinAgreement,
        @Schema(description = "深度模式维度→模型映射") Map<String, List<Map<String, Object>>> dimensionReviewers
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
                riskFlagsForceManual, true, new BigDecimal("0.20"), "auto", 5, false,
                null, null, null, null);
    }
}
