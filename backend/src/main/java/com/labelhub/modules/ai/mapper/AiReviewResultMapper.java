package com.labelhub.modules.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.ai.domain.AiReviewResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiReviewResultMapper extends BaseMapper<AiReviewResult> {

    @Select("""
            SELECT *
            FROM ai_review_results
            WHERE submission_id = #{submissionId}
            LIMIT 1
            """)
    AiReviewResult selectBySubmissionId(@Param("submissionId") Long submissionId);
}
