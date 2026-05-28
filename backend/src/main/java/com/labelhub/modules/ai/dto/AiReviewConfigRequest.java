package com.labelhub.modules.ai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
        @NotEmpty Map<String, Object> outputSchema
) {
}
