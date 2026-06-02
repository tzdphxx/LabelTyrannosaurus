package com.labelhub.modules.ai.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiDecisionTest {

    @Test
    void aiReviewDecisionUsesPassRejectAndManualReview() {
        assertThat(AiDecision.values())
                .extracting(Enum::name)
                .containsExactly("PASS", "REJECT", "MANUAL_REVIEW");
        assertThat(AiDecision.valueOf("REJECT")).isEqualTo(AiDecision.REJECT);
    }
}
