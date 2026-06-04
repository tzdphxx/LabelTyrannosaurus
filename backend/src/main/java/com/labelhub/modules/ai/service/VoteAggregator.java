package com.labelhub.modules.ai.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 多模型并行投票结果汇总器。
 * 将 2-3 个模型的审核结果合并为单一结果。
 */
@Component
public class VoteAggregator {

    private static final Logger log = LoggerFactory.getLogger(VoteAggregator.class);

    /**
     * 汇总多个投票结果。
     *
     * @param results 各模型的审核结果（结构化 JSON）
     * @param minAgreement 最少一致票数
     * @return 汇总后的结果
     */
    public AggregatedResult aggregate(List<Map<String, Object>> results, int minAgreement) {
        if (results == null || results.isEmpty()) {
            return AggregatedResult.EMPTY;
        }
        if (results.size() == 1) {
            return fromSingle(results.get(0));
        }

        // 决策投票
        Map<String, Long> decisionVotes = results.stream()
                .map(r -> stringValue(r.get("decision"), "UNCERTAIN"))
                .filter(d -> !d.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        String winningDecision = decisionVotes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNCERTAIN");

        long topVotes = decisionVotes.getOrDefault(winningDecision, 0L);
        boolean hasConsensus = topVotes >= minAgreement;

        // 加权平均分数
        double totalWeight = 0;
        double weightedScore = 0;
        List<Double> scores = new ArrayList<>();
        for (Map<String, Object> r : results) {
            double score = doubleValue(r.get("averageScore"), 0);
            double confidence = doubleValue(r.get("confidence"), 0.5);
            if (confidence <= 0) confidence = 0.5;
            weightedScore += score * confidence;
            totalWeight += confidence;
            scores.add(score);
        }

        double averageScore = totalWeight > 0
                ? weightedScore / totalWeight : 0;

        // 置信度：评分标准差越小越高
        double stdDev = standardDeviation(scores);
        double aggregatedConfidence = hasConsensus
                ? Math.max(0, 1.0 - stdDev / 2.0)
                : Math.max(0, 0.5 - stdDev / 2.0);

        // 风险标记：合集去重
        List<String> riskFlags = new ArrayList<>();
        for (Map<String, Object> r : results) {
            Object flags = r.get("riskFlags");
            if (flags instanceof List<?> list) {
                for (Object f : list) {
                    String s = String.valueOf(f);
                    if (!s.isBlank() && !riskFlags.contains(s)) {
                        riskFlags.add(s);
                    }
                }
            }
        }

        // 建议文本：取最高置信度模型的建议
        String suggestion = "";
        double bestConf = -1;
        for (Map<String, Object> r : results) {
            double c = doubleValue(r.get("confidence"), 0);
            if (c > bestConf) {
                bestConf = c;
                suggestion = stringValue(r.get("suggestion"), "");
            }
        }

        // 维度分数：取各模型维度的平均值
        Map<String, Object> dimensionScores = new LinkedHashMap<>();
        Map<String, List<Double>> dimCollector = new LinkedHashMap<>();
        for (Map<String, Object> r : results) {
            Object ds = r.get("dimensionScores");
            if (ds instanceof Map<?, ?> dimMap) {
                for (Map.Entry<?, ?> e : dimMap.entrySet()) {
                    String key = String.valueOf(e.getKey());
                    double v = doubleValue(e.getValue(), 0);
                    dimCollector.computeIfAbsent(key, k -> new ArrayList<>()).add(v);
                }
            }
        }
        for (Map.Entry<String, List<Double>> e : dimCollector.entrySet()) {
            double avg = e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            dimensionScores.put(e.getKey(), BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("decision", winningDecision);
        result.put("averageScore", BigDecimal.valueOf(averageScore).setScale(2, RoundingMode.HALF_UP));
        result.put("confidence", BigDecimal.valueOf(aggregatedConfidence).setScale(4, RoundingMode.HALF_UP));
        result.put("dimensionScores", dimensionScores);
        result.put("riskFlags", riskFlags);
        result.put("suggestion", suggestion);
        result.put("_voteCount", results.size());
        result.put("_topVotes", topVotes);
        result.put("_hasConsensus", hasConsensus);

        log.debug("Vote aggregation: {} voters, decision={}, consensus={}", results.size(), winningDecision, hasConsensus);
        return new AggregatedResult(result, hasConsensus, (int) topVotes);
    }

    private AggregatedResult fromSingle(Map<String, Object> single) {
        Map<String, Object> result = new LinkedHashMap<>(single);
        result.put("_voteCount", 1);
        result.put("_topVotes", 1L);
        result.put("_hasConsensus", true);
        return new AggregatedResult(result, true, 1);
    }

    private static String stringValue(Object value, String fallback) {
        if (value == null) return fallback;
        String s = String.valueOf(value);
        return s.isBlank() ? fallback : s;
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

    private static double standardDeviation(List<Double> values) {
        if (values.size() <= 1) return 0;
        double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - avg, 2)).average().orElse(0);
        return Math.sqrt(variance);
    }

    public record AggregatedResult(Map<String, Object> resultJson, boolean hasConsensus, int topVotes) {
        public static final AggregatedResult EMPTY = new AggregatedResult(Map.of(), false, 0);
    }
}
