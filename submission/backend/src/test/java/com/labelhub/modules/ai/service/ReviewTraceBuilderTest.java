package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.labelhub.modules.ai.dto.ReviewTraceResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReviewTraceBuilderTest {

    private final ReviewTraceBuilder builder = new ReviewTraceBuilder();

    @Test
    void directTraceDescribesSingleModelReview() {
        ReviewTraceResponse trace = builder.direct("qwen-plus", "PASS",
                new BigDecimal("92.5"), new BigDecimal("0.88"));

        assertThat(trace.strategy()).isEqualTo("LIGHTWEIGHT");
        assertThat(trace.steps()).hasSize(1);
        assertThat(trace.summary()).contains("Single model");
        assertThat(trace.metrics()).containsEntry("reviewerCount", 1);
    }

    @Test
    void voteTraceIncludesConsensusMetrics() {
        ReviewTraceResponse trace = builder.parallelVote(
                List.of(
                        ReviewTraceBuilder.step("qwen-a", "voter", "PASS", "90", "0.9", "SUCCESS", "pass"),
                        ReviewTraceBuilder.step("qwen-b", "voter", "PASS", "86", "0.8", "SUCCESS", "pass"),
                        ReviewTraceBuilder.step("qwen-c", "voter", "MANUAL_REVIEW", "70", "0.6", "SUCCESS", "uncertain")
                ),
                Map.of("voteCount", 3, "topVotes", 2, "hasConsensus", true, "minAgreement", 2)
        );

        assertThat(trace.strategy()).isEqualTo("PARALLEL_VOTE");
        assertThat(trace.metrics()).containsEntry("voteCount", 3);
        assertThat(trace.summary()).contains("3 model branches").contains("2 branch");
    }
}
