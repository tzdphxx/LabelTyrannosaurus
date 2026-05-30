package com.labelhub.modules.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.ai.domain.AiFlowAction;
import com.labelhub.modules.ai.domain.AiFlowPolicy;
import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.domain.AiReviewResult;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiFlowDecisionService {

    private static final BigDecimal DEFAULT_CONFIDENCE_THRESHOLD = new BigDecimal("0.85");
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiFlowAction decide(AiReviewResult result, AiReviewConfig config) {
        AiFlowPolicy policy = resolvePolicy(config);

        if (policy == AiFlowPolicy.ALWAYS_MANUAL || policy == AiFlowPolicy.MANUAL_FIRST) {
            return AiFlowAction.AI_ASSIGN_MANUAL_REVIEW;
        }

        if (hasForceManualRiskFlags(result, config)) {
            return AiFlowAction.AI_ASSIGN_MANUAL_REVIEW;
        }

        String decision = result.getDecision();
        if (decision == null) {
            return AiFlowAction.AI_ASSIGN_MANUAL_REVIEW;
        }

        BigDecimal confidence = result.getConfidence();
        BigDecimal confidenceThreshold = config.getConfidenceThreshold() != null
                ? config.getConfidenceThreshold() : DEFAULT_CONFIDENCE_THRESHOLD;

        if (confidence == null || confidence.compareTo(confidenceThreshold) < 0) {
            return AiFlowAction.AI_ASSIGN_MANUAL_REVIEW;
        }

        return switch (decision) {
            case "PASS" -> decideForPass(result, config, policy);
            case "REJECT" -> decideForReject(result, config, policy);
            default -> AiFlowAction.AI_ASSIGN_MANUAL_REVIEW;
        };
    }

    private AiFlowAction decideForPass(AiReviewResult result, AiReviewConfig config,
                                       AiFlowPolicy policy) {
        if (policy != AiFlowPolicy.AI_PASS_ONLY && policy != AiFlowPolicy.AI_PASS_AND_REJECT) {
            return AiFlowAction.AI_ASSIGN_MANUAL_REVIEW;
        }
        if (!Boolean.TRUE.equals(config.getAllowAiDirectApprove())) {
            return AiFlowAction.AI_ASSIGN_MANUAL_REVIEW;
        }
        BigDecimal passThreshold = config.getPassThreshold();
        BigDecimal avgScore = result.getAverageScore();
        if (passThreshold != null && avgScore != null
                && avgScore.compareTo(passThreshold) < 0) {
            return AiFlowAction.AI_ASSIGN_MANUAL_REVIEW;
        }
        return AiFlowAction.AI_DIRECT_APPROVE;
    }

    private AiFlowAction decideForReject(AiReviewResult result, AiReviewConfig config,
                                         AiFlowPolicy policy) {
        if (policy != AiFlowPolicy.AI_REJECT_ONLY && policy != AiFlowPolicy.AI_PASS_AND_REJECT) {
            return AiFlowAction.AI_ASSIGN_MANUAL_REVIEW;
        }
        if (!Boolean.TRUE.equals(config.getAllowAiDirectReject())) {
            return AiFlowAction.AI_ASSIGN_MANUAL_REVIEW;
        }
        BigDecimal rejectThreshold = config.getRejectThreshold();
        BigDecimal avgScore = result.getAverageScore();
        if (rejectThreshold != null && avgScore != null
                && avgScore.compareTo(rejectThreshold) > 0) {
            return AiFlowAction.AI_ASSIGN_MANUAL_REVIEW;
        }
        return AiFlowAction.AI_DIRECT_REJECT;
    }

    private AiFlowPolicy resolvePolicy(AiReviewConfig config) {
        if (config.getAiFlowPolicy() == null) {
            return AiFlowPolicy.MANUAL_FIRST;
        }
        try {
            return AiFlowPolicy.valueOf(config.getAiFlowPolicy());
        } catch (IllegalArgumentException e) {
            return AiFlowPolicy.MANUAL_FIRST;
        }
    }

    private boolean hasForceManualRiskFlags(AiReviewResult result, AiReviewConfig config) {
        String riskFlagsJson = result.getRiskFlags();
        String forceManualJson = config.getRiskFlagsForceManual();
        if (riskFlagsJson == null || forceManualJson == null) {
            return false;
        }
        try {
            List<String> riskFlags = objectMapper.readValue(riskFlagsJson, STRING_LIST);
            List<String> forceManual = objectMapper.readValue(forceManualJson, STRING_LIST);
            return riskFlags.stream().anyMatch(forceManual::contains);
        } catch (Exception e) {
            return false;
        }
    }
}
