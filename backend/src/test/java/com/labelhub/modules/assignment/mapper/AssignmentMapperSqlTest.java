package com.labelhub.modules.assignment.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

class AssignmentMapperSqlTest {

    @Test
    void claimedItemsReturnedInfoOnlyUsesCurrentReturnedSubmission() throws NoSuchMethodException {
        Method method = AssignmentMapper.class.getMethod(
                "selectLabelerClaimedItems",
                Long.class,
                Long.class,
                String.class,
                int.class,
                int.class);
        String sql = String.join("\n", method.getAnnotation(Select.class).value());

        assertThat(normalize(sql))
                .contains("CASE WHEN a.status IN ('RETURNED', 'AI_RETURNED') THEN")
                .contains("rs.status != 'SUPERSEDED'")
                .contains("ELSE NULL END AS returned_reason")
                .contains("ELSE NULL END AS returned_at");
    }

    private String normalize(String sql) {
        return String.join(" ", Arrays.stream(sql.split("\\s+")).toList());
    }
}
