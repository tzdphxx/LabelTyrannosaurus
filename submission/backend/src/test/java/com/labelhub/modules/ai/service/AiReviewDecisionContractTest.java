package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AiReviewDecisionContractTest {

    @Test
    void structuredOutputSchemaUsesDatabaseDecisionValues() {
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) AiReviewSchema.SCHEMA.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> decision = (Map<String, Object>) properties.get("decision");

        assertThat(decision.get("enum"))
                .isEqualTo(List.of("PASS", "REJECT", "MANUAL_REVIEW"));
    }

    @Test
    void dimensionAggregatorUsesManualReviewForMiddleScoreBand() {
        DimensionAggregator aggregator = new DimensionAggregator(new VoteAggregator());
        Map<String, List<Map<String, Object>>> dimensionResults = Map.of(
                "accuracy", List.of(Map.of(
                        "decision", "MANUAL_REVIEW",
                        "averageScore", 70,
                        "confidence", 0.8,
                        "dimensionScores", Map.of("accuracy", 70),
                        "riskFlags", List.of(),
                        "suggestion", "needs review"
                ))
        );

        Map<String, Object> result = aggregator.aggregate(dimensionResults, 1, 80, 60);

        assertThat(result.get("decision")).isEqualTo("MANUAL_REVIEW");
    }

    @Test
    void voteAggregatorDefaultsMissingDecisionToManualReview() {
        VoteAggregator.AggregatedResult result = new VoteAggregator().aggregate(List.of(
                Map.of("averageScore", 50, "confidence", 0.8),
                Map.of("decision", "PASS", "averageScore", 90, "confidence", 0.8)
        ), 2);

        assertThat(result.resultJson().get("decision")).isEqualTo("MANUAL_REVIEW");
    }
}
