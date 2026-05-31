package com.labelhub.modules.review.mapper;

import com.labelhub.modules.review.dto.ReviewerSubmissionListItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReviewerSubmissionListMapper {

    @Select("""
            <script>
            SELECT s.id AS submissionId, s.task_id AS taskId,
                   s.dataset_item_id AS datasetItemId, s.labeler_id AS labelerId,
                   s.status AS submissionStatus,
                   ar.status AS aiReviewStatus, ar.decision AS aiDecision,
                   cg.status AS conflictStatus,
                   s.current_review_level AS reviewLevel,
                   s.assigned_reviewer_id AS assignedReviewerId,
                   s.submitted_at AS createdAt, s.updated_at AS updatedAt
            FROM submissions s
            LEFT JOIN ai_review_results ar ON ar.submission_id = s.id
            LEFT JOIN conflict_groups cg ON cg.task_id = s.task_id
                   AND cg.dataset_item_id = s.dataset_item_id
            WHERE s.status NOT IN ('SUPERSEDED', 'APPROVED', 'REJECTED', 'AI_REVIEWING')
            <if test="taskId != null"> AND s.task_id = #{taskId}</if>
            <if test="submissionStatus != null"> AND s.status = #{submissionStatus}</if>
            <if test="aiDecision != null"> AND ar.decision = #{aiDecision}</if>
            <if test="aiReviewStatus != null"> AND ar.status = #{aiReviewStatus}</if>
            <if test="conflictStatus != null"> AND cg.status = #{conflictStatus}</if>
            <if test="reviewLevel != null"> AND s.current_review_level = #{reviewLevel}</if>
            <if test="assignedReviewerId != null"> AND s.assigned_reviewer_id = #{assignedReviewerId}</if>
            ORDER BY s.submitted_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ReviewerSubmissionListItem> selectWithFilters(
            @Param("taskId") Long taskId,
            @Param("submissionStatus") String submissionStatus,
            @Param("aiDecision") String aiDecision,
            @Param("aiReviewStatus") String aiReviewStatus,
            @Param("conflictStatus") String conflictStatus,
            @Param("reviewLevel") Integer reviewLevel,
            @Param("assignedReviewerId") Long assignedReviewerId,
            @Param("offset") int offset,
            @Param("limit") int limit);
}
