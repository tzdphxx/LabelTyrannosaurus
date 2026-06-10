package com.labelhub.modules.review.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

class ReviewerSubmissionListMapperSqlTest {

    @Test
    void reviewTaskMarketplaceSelectsPublishedTasksWithoutExposingReviewLevelRows()
            throws NoSuchMethodException {
        Method method = ReviewerSubmissionListMapper.class.getMethod(
                "selectReviewTasksForReviewer", Long.class, String.class);
        String sql = normalize(String.join("\n", method.getAnnotation(Select.class).value()));

        assertThat(sql)
                .contains("from tasks t")
                .contains("where t.status = 'published'")
                .contains("from review_task_claims")
                .contains("case x.claimstatus")
                .contains("and x.claimstatus = #{claimscope}")
                .doesNotContain("as reviewlevel");
    }

    private String normalize(String sql) {
        return String.join(" ", Arrays.stream(sql.split("\\s+")).toList()).toLowerCase();
    }
}
