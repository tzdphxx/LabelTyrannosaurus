package com.labelhub.modules.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.review.domain.ReviewRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReviewRecordMapper extends BaseMapper<ReviewRecord> {

    @Select("""
            <script>
            SELECT *
            FROM review_records
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
            ORDER BY submission_id ASC, created_at DESC
            </script>
            """)
    List<ReviewRecord> selectBySubmissionIds(@Param("submissionIds") List<Long> submissionIds);

    @Select("""
            SELECT COUNT(1)
            FROM review_records
            WHERE reviewer_id = #{reviewerId}
              AND action IN ('APPROVE', 'REJECT')
              AND DATE(created_at) = CURDATE()
            """)
    int countTodayReviewed(@Param("reviewerId") Long reviewerId);

    @Select("""
            SELECT COUNT(1)
            FROM review_records
            WHERE reviewer_id = #{reviewerId}
              AND action = 'APPROVE'
            """)
    int countTotalApproved(@Param("reviewerId") Long reviewerId);

    @Select("""
            SELECT COUNT(1)
            FROM review_records
            WHERE reviewer_id = #{reviewerId}
              AND action = 'REJECT'
            """)
    int countTotalRejected(@Param("reviewerId") Long reviewerId);

    @Select("""
            SELECT COUNT(1)
            FROM review_records
            WHERE submission_id = #{submissionId}
              AND reviewer_id = #{reviewerId}
              AND review_level <> #{excludeLevel}
              AND action IN ('APPROVE', 'REJECT')
            """)
    int countBySubmissionAndReviewerExcludingLevel(@Param("submissionId") Long submissionId,
                                                   @Param("reviewerId") Long reviewerId,
                                                   @Param("excludeLevel") int excludeLevel);
}
