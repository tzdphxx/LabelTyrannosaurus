package com.labelhub.modules.review.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewActionTest {

    @Test
    void reviewActionSeparatesHumanRejectFromAiDirectReject() {
        assertThat(ReviewAction.values())
                .extracting(Enum::name)
                .contains("REJECT", "AI_DIRECT_REJECT")
                .doesNotContain("AI_REJECT");
    }
}
