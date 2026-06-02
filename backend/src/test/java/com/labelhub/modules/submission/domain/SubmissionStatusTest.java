package com.labelhub.modules.submission.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionStatusTest {

    @Test
    void submissionStatusMatchesContract() {
        assertThat(SubmissionStatus.values())
                .extracting(Enum::name)
                .containsExactly(
                        "SUBMITTED",
                        "AI_REVIEWING",
                        "PENDING_FINAL",
                        "APPROVED",
                        "REJECTED",
                        "SUPERSEDED");
    }
}
