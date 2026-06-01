package com.labelhub.modules.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.review.domain.ReviewTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReviewTaskMapper extends BaseMapper<ReviewTask> {

    @Select("""
            SELECT * FROM review_tasks
            WHERE submission_id = #{submissionId}
              AND review_level = #{level}
            LIMIT 1
            """)
    ReviewTask selectBySubmissionAndLevel(@Param("submissionId") Long submissionId,
                                          @Param("level") int level);

    @Select("""
            SELECT COUNT(1) FROM review_tasks
            WHERE submission_id = #{submissionId}
              AND assigned_reviewer_id = #{reviewerId}
              AND status <> 'CANCELLED'
            """)
    int countBySubmissionAndReviewer(@Param("submissionId") Long submissionId,
                                     @Param("reviewerId") Long reviewerId);
}
