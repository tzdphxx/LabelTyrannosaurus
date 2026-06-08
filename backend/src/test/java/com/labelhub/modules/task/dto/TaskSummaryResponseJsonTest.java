package com.labelhub.modules.task.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.task.domain.ClaimStrategy;
import com.labelhub.modules.task.domain.TaskStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaskSummaryResponseJsonTest {

    @Test
    void serializesMaxClaimsPerLabelerAsSnakeCase() throws JsonProcessingException {
        TaskSummaryResponse response = new TaskSummaryResponse(
                10L,
                "Quota task",
                TaskStatus.PUBLISHED,
                List.of("qa"),
                100,
                20,
                1,
                ClaimStrategy.QUOTA_GRAB,
                8,
                null,
                null,
                null,
                null,
                null
        );

        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json).contains("\"max_claims_per_labeler\":8");
        assertThat(json).doesNotContain("maxClaimsPerLabeler");
    }
}
