package com.labelhub.modules.ai.dto;

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

public record AiReviewConfigRequest(
        @NotNull Long providerId,
        @NotBlank @Size(max = 128) String modelName,
        @NotBlank @Size(max = 10000) String promptTemplate,
        @NotEmpty List<@NotBlank @Size(max = 64) String> scoringDimensions,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal passThreshold,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal manualReviewThreshold,
        @NotEmpty Map<String, Object> outputSchema,
        @Min(0) @Max(10) Integer maxRetry,
        String aiFlowPolicy,
        Boolean allowAiDirectApprove,
        Boolean allowAiDirectReject,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal rejectThreshold,
        @DecimalMin("0.00") @DecimalMax("1.00") BigDecimal confidenceThreshold,
        List<String> riskFlagsForceManual,
        Boolean multimodalEnabled,
        @DecimalMin("0.00") @DecimalMax("1.00") BigDecimal degradationPenalty,
        @Size(max = 20) String visionDetail,
        @Min(0) @Max(20) Integer maxImagesPerRequest,
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
