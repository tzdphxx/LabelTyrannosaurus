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
        String promptVersion
) {
}
