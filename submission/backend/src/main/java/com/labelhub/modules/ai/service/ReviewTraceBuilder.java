package com.labelhub.modules.ai.service;

import com.labelhub.modules.ai.dto.ReviewTraceResponse;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReviewTraceBuilder {

    public ReviewTraceResponse direct(String modelName, String decision, BigDecimal score, BigDecimal confidence) {
        ReviewTraceResponse.ReviewTraceStep step = step(
                modelName, "single_reviewer", decision, text(score), text(confidence), "SUCCESS",
                "Single model structured review");
        return new ReviewTraceResponse(
                "LIGHTWEIGHT",
                "Single model review",
                "Single model produced the final structured review result.",
                List.of(step),
                Map.of("reviewerCount", 1)
        );
    }

    public ReviewTraceResponse parallelVote(List<ReviewTraceResponse.ReviewTraceStep> steps,
                                            Map<String, Object> metrics) {
        int voteCount = intMetric(metrics, "voteCount");
        int topVotes = intMetric(metrics, "topVotes");
        boolean hasConsensus = Boolean.TRUE.equals(metrics == null ? null : metrics.get("hasConsensus"));
        String summary = voteCount + " model branches reviewed in parallel; " + topVotes
                + " branch(es) supported the final decision; "
                + (hasConsensus ? "consensus threshold was met." : "consensus threshold was not met.");
        return new ReviewTraceResponse("PARALLEL_VOTE", "Parallel model vote", summary, steps, ordered(metrics));
    }

    public ReviewTraceResponse deepDimension(List<ReviewTraceResponse.ReviewTraceStep> steps,
                                             Map<String, Object> metrics) {
        int dimensionCount = intMetric(metrics, "dimensionCount");
        String summary = "Review was split into " + dimensionCount
                + " scoring dimension(s), then aggregated into a final decision.";
        return new ReviewTraceResponse("DEEP_DIMENSION", "Dimension-specialized review", summary, steps, ordered(metrics));
    }

    public ReviewTraceResponse supervisor(String strategy,
                                          List<ReviewTraceResponse.ReviewTraceStep> steps,
                                          Map<String, Object> metrics) {
        String resolvedStrategy = strategy == null || strategy.isBlank() ? "AGENT_DEBATE" : strategy;
        String label = "AGENT_DEBATE".equals(resolvedStrategy) ? "Agent debate review" : "Supervisor tool review";
        return new ReviewTraceResponse(
                resolvedStrategy,
                label,
                label + " completed with summarized reasoning and execution metrics.",
                steps,
                ordered(metrics)
        );
    }

    public static ReviewTraceResponse.ReviewTraceStep step(String name,
                                                           String role,
                                                           String decision,
                                                           String score,
                                                           String confidence,
                                                           String status,
                                                           String reason) {
        return new ReviewTraceResponse.ReviewTraceStep(name, role, decision, score, confidence, status, reason);
    }

    private static Map<String, Object> ordered(Map<String, Object> source) {
        return source == null ? Map.of() : new LinkedHashMap<>(source);
    }

    private static int intMetric(Map<String, Object> metrics, String key) {
        Object value = metrics == null ? null : metrics.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String text(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
