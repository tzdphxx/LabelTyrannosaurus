package com.labelhub.modules.ai.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AiReviewConfigResponse(
        Long id,
        Long taskId,
        Long providerId,
        String modelName,
        String promptTemplate,
        List<String> scoringDimensions,
        BigDecimal passThreshold,
        BigDecimal manualReviewThreshold,
        Map<String, Object> outputSchema,
        String promptVersion,
        Integer maxRetry,
        String aiFlowPolicy,
        Boolean allowAiDirectApprove,
        Boolean allowAiDirectReject,
        BigDecimal rejectThreshold,
        BigDecimal confidenceThreshold,
        List<String> riskFlagsForceManual,
        Boolean multimodalEnabled,
        BigDecimal degradationPenalty,
        String visionDetail,
        Integer maxImagesPerRequest,
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
