package com.labelhub.modules.reward.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

class RewardRuleRepositoryMapperSqlTest {

    @Test
    void latestByTaskIdsSelectsLatestRulePerTask() throws NoSuchMethodException {
        Method method = RewardRuleRepositoryMapper.class.getMethod(
                "selectLatestByTaskIds",
                Collection.class);
        String sql = String.join("\n", method.getAnnotation(Select.class).value());

        assertThat(normalize(sql))
                .contains("from reward_rules rr")
                .contains("select task_id, max(effective_version) as effective_version")
                .contains("where task_id in")
                .contains("group by task_id")
                .contains("rr.task_id = latest.task_id")
                .contains("rr.effective_version = latest.effective_version");
    }

    private String normalize(String sql) {
        return String.join(" ", Arrays.stream(sql.split("\\s+")).toList());
    }
}
