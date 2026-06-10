package com.labelhub.modules.ai.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SupervisorResult(
        boolean success,
        String decision,
        BigDecimal averageScore,
        Map<String, Object> dimensionScores,
        List<String> riskFlags,
        String suggestion,
        String rawConversation,
        String errorCode,
        String errorMessage
) {

    public static SupervisorResult success(String decision, BigDecimal averageScore,
                                           Map<String, Object> dimensionScores,
                                           List<String> riskFlags, String suggestion,
                                           String rawConversation) {
        return new SupervisorResult(true, decision, averageScore, dimensionScores,
                riskFlags, suggestion, rawConversation, null, null);
    }

    public static SupervisorResult failure(String errorCode, String errorMessage, String rawConversation) {
        return new SupervisorResult(false, null, null, null, null, null, rawConversation, errorCode, errorMessage);
    }
}
