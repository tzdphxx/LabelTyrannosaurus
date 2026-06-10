package com.labelhub.modules.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.review.domain.ReviewTaskClaim;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReviewTaskClaimMapper extends BaseMapper<ReviewTaskClaim> {

    @Select("""
            SELECT *
            FROM review_task_claims
            WHERE task_id = #{taskId} AND review_level = #{reviewLevel}
            LIMIT 1
            """)
    ReviewTaskClaim selectByTaskAndLevel(@Param("taskId") Long taskId,
                                         @Param("reviewLevel") Integer reviewLevel);

    @Select("""
            SELECT *
            FROM review_task_claims
            WHERE task_id = #{taskId}
            ORDER BY review_level ASC
            """)
    List<ReviewTaskClaim> selectByTask(@Param("taskId") Long taskId);

    @Select("""
            SELECT reviewer_id
            FROM review_task_claims
            WHERE task_id = #{taskId} AND review_level = #{reviewLevel}
            LIMIT 1
            """)
    Long selectReviewerForTaskLevel(@Param("taskId") Long taskId,
                                    @Param("reviewLevel") Integer reviewLevel);

    @Select("""
            SELECT COUNT(1)
            FROM review_task_claims
            WHERE task_id = #{taskId}
              AND reviewer_id <> #{reviewerId}
            """)
    int countOtherLevelClaimsByOthers(@Param("taskId") Long taskId,
                                      @Param("reviewerId") Long reviewerId);
}
