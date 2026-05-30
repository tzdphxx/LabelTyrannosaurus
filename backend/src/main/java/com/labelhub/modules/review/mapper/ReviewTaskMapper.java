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
}
