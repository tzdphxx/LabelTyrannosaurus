package com.labelhub.modules.task.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.reward.dto.RewardRuleResponse;
import com.labelhub.modules.task.domain.ClaimStrategy;
import com.labelhub.modules.task.domain.TaskStatus;
import java.math.BigDecimal;
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
                null,
                null
        );

        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json).contains("\"max_claims_per_labeler\":8");
        assertThat(json).doesNotContain("maxClaimsPerLabeler");
    }

    @Test
    void serializesRewardRuleAsNestedObject() throws JsonProcessingException {
        TaskSummaryResponse response = new TaskSummaryResponse(
                10L,
                "Reward task",
                TaskStatus.PUBLISHED,
                List.of("qa"),
                100,
                20,
                1,
                ClaimStrategy.FCFS,
                null,
                null,
                null,
                null,
                null,
                null,
                new RewardRuleResponse(
                        101L,
                        10L,
                        2,
                        "APPROVED_ITEM",
                        new BigDecimal("3.50"),
                        "POINT",
                        true,
                        null,
                        1L,
                        null
                )
        );

        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json).contains("\"rewardRule\":");
        assertThat(json).contains("\"ruleId\":101");
        assertThat(json).contains("\"unitReward\":3.50");
    }
}
