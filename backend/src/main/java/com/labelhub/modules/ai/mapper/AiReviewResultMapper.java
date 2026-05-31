package com.labelhub.modules.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.ai.domain.AiReviewResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AiReviewResultMapper extends BaseMapper<AiReviewResult> {

    @Select("""
            SELECT *
            FROM ai_review_results
            WHERE submission_id = #{submissionId}
            LIMIT 1
            """)
    AiReviewResult selectBySubmissionId(@Param("submissionId") Long submissionId);

    @Select("""
            <script>
            SELECT *
            FROM ai_review_results
            <choose>
            <when test="submissionIds != null and submissionIds.size() > 0">
            WHERE submission_id IN
            <foreach collection="submissionIds" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            </when>
            <otherwise>
            WHERE 1 = 0
            </otherwise>
            </choose>
            </script>
            """)
    List<AiReviewResult> selectBySubmissionIds(@Param("submissionIds") List<Long> submissionIds);

    @Select("""
            SELECT *
            FROM ai_review_results
            WHERE status IN ('FAILED', 'RATE_LIMITED')
              AND next_retry_at IS NOT NULL
              AND next_retry_at <= NOW()
            """)
    List<AiReviewResult> selectPendingRetries();

    @Update("""
            UPDATE ai_review_results
            SET status = #{status},
                retry_count = #{retryCount},
                next_retry_at = #{nextRetryAt},
                effective_run_id = #{effectiveRunId},
                error_code = #{errorCode},
                error_message = #{errorMessage},
                raw_response = #{rawResponse},
                updated_at = NOW(3)
            WHERE submission_id = #{submissionId}
              AND retry_count = #{expectedRetryCount}
            """)
    int updateForRetry(@Param("submissionId") Long submissionId,
                       @Param("expectedRetryCount") int expectedRetryCount,
                       @Param("status") String status,
                       @Param("retryCount") int retryCount,
                       @Param("nextRetryAt") LocalDateTime nextRetryAt,
                       @Param("effectiveRunId") Long effectiveRunId,
                       @Param("errorCode") String errorCode,
                       @Param("errorMessage") String errorMessage,
                       @Param("rawResponse") String rawResponse);

    @Update("""
            UPDATE ai_review_results
            SET status = #{status},
                effective_run_id = #{effectiveRunId},
                decision = #{decision},
                average_score = #{averageScore},
                dimension_scores = #{dimensionScores},
                risk_flags = #{riskFlags},
                suggestion = #{suggestion},
                raw_response = #{rawResponse},
                error_code = NULL,
                error_message = NULL,
                next_retry_at = NULL,
                updated_at = NOW(3)
            WHERE submission_id = #{submissionId}
            """)
    int updateForSuccess(@Param("submissionId") Long submissionId,
                         @Param("status") String status,
                         @Param("effectiveRunId") Long effectiveRunId,
                         @Param("decision") String decision,
                         @Param("averageScore") BigDecimal averageScore,
                         @Param("dimensionScores") String dimensionScores,
                         @Param("riskFlags") String riskFlags,
                         @Param("suggestion") String suggestion,
                         @Param("rawResponse") String rawResponse);
}
