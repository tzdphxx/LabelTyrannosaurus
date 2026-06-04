package com.labelhub.modules.ai.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 维度专项审核汇总器。
 * 将每个评分维度的多个模型投票结果合并为整体审核结论。
 */
@Component
public class DimensionAggregator {

    private static final Logger log = LoggerFactory.getLogger(DimensionAggregator.class);

    private final VoteAggregator voteAggregator;

    public DimensionAggregator(VoteAggregator voteAggregator) {
        this.voteAggregator = voteAggregator;
    }

    /**
     * 汇总维度专项审核结果。
     *
     * @param dimensionResults 维度名 → 该维度的多个模型结果列表
     * @param minAgreement 维度内最少一致票数
     * @return 汇总结果
     */
    public Map<String, Object> aggregate(Map<String, List<Map<String, Object>>> dimensionResults,
                                          int minAgreement, double passThreshold, double manualReviewThreshold) {
        Map<String, Object> dimensionScores = new LinkedHashMap<>();
        Map<String, Double> dimensionConfidences = new LinkedHashMap<>();
        List<String> allRiskFlags = new ArrayList<>();
        List<String> limitations = new ArrayList<>();
        StringBuilder suggestion = new StringBuilder();

        for (Map.Entry<String, List<Map<String, Object>>> entry : dimensionResults.entrySet()) {
            String dimName = entry.getKey();
            List<Map<String, Object>> dimResults = entry.getValue();

            if (dimResults == null || dimResults.isEmpty()) {
                log.warn("Dimension '{}' has no reviewer results, skipping", dimName);
                continue;
            }

            // 维度内投票
            VoteAggregator.AggregatedResult dimVote = voteAggregator.aggregate(dimResults, minAgreement);
            Map<String, Object> dimJson = dimVote.resultJson();

            // 提取维度分数（从 dimensionScores 或 averageScore 获取）
            Object dimScores = dimJson.get("dimensionScores");
            if (dimScores instanceof Map<?, ?> scoresMap) {
                Object score = scoresMap.get(dimName);
                if (score != null) {
                    dimensionScores.put(dimName, toDecimal(score));
                }
            }
            if (!dimensionScores.containsKey(dimName)) {
                double avgScore = doubleValue(dimJson.get("averageScore"), 0);
                dimensionScores.put(dimName, BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP));
            }

            // 维度置信度
            double confidence = doubleValue(dimJson.get("confidence"), 0.5);
            dimensionConfidences.put(dimName, confidence);

            // 风险标记合集
            Object flags = dimJson.get("riskFlags");
            if (flags instanceof List<?> list) {
                for (Object f : list) {
                    String s = String.valueOf(f);
                    if (!s.isBlank() && !allRiskFlags.contains(s)) {
                        allRiskFlags.add(s);
                    }
                }
            }

            // 维度 limitations 聚合
            Object dimLimits = dimJson.get("limitations");
            if (dimLimits instanceof List<?> dimLimitsList) {
                for (Object item : dimLimitsList) {
                    String s = String.valueOf(item);
                    if (!s.isBlank() && !limitations.contains(s)) {
                        limitations.add(s);
                    }
                }
            }

            // 建议聚合
            String dimSuggestion = stringValue(dimJson.get("suggestion"), "");
            if (!dimSuggestion.isBlank()) {
                suggestion.append("[").append(dimName).append("] ").append(dimSuggestion).append("; ");
            }
        }

        // 整体平均分数（各维度等权平均）
        if (dimensionScores.isEmpty()) {
            log.warn("All {} dimensions returned empty results; decision will be REJECT", dimensionResults.size());
        }
        double overallScore = dimensionScores.values().stream()
                .mapToDouble(v -> v instanceof Number n ? n.doubleValue() : 0)
                .average().orElse(0);

        // 整体置信度（各维度置信度的调和平均）
        double overallConfidence = dimensionConfidences.values().stream()
                .mapToDouble(Double::doubleValue)
                .average().orElse(0);

        // 整体决策 — 使用任务配置的阈值
        String decision = overallScore >= passThreshold ? "PASS"
                : overallScore >= manualReviewThreshold ? "UNCERTAIN" : "REJECT";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("decision", decision);
        result.put("averageScore", BigDecimal.valueOf(overallScore).setScale(2, RoundingMode.HALF_UP));
        result.put("confidence", BigDecimal.valueOf(overallConfidence).setScale(4, RoundingMode.HALF_UP));
        result.put("dimensionScores", dimensionScores);
        result.put("riskFlags", allRiskFlags);
        result.put("suggestion", suggestion.toString().trim());
        result.put("limitations", limitations);

        log.debug("Dimension aggregation: {} dimensions, overallScore={}, decision={}",
                dimensionScores.size(), overallScore, decision);
        return result;
    }

    private static BigDecimal toDecimal(Object value) {
        if (value instanceof BigDecimal d) return d;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static double doubleValue(Object value, double fallback) {
        if (value instanceof Number n) return n.doubleValue();
        if (value == null) return fallback;
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String stringValue(Object value, String fallback) {
        if (value == null) return fallback;
        String s = String.valueOf(value);
        return s.isBlank() ? fallback : s;
    }
}
