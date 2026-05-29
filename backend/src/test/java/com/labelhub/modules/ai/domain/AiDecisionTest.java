package com.labelhub.modules.ai.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiDecisionTest {

    @Test
    void aiReviewDecisionUsesPassReturnAndManualReview() {
        assertThat(AiDecision.values())
                .extracting(Enum::name)
                .containsExactly("PASS", "RETURN", "MANUAL_REVIEW");
        assertThat(AiDecision.valueOf("RETURN")).isEqualTo(AiDecision.RETURN);
    }
}
