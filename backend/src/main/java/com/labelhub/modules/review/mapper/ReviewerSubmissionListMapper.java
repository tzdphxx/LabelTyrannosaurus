package com.labelhub.modules.review.mapper;

import com.labelhub.modules.review.dto.ReviewerAiReviewStatusItem;
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
            <if test="scope != null and scope == 'CLAIMED'.toString()"> AND s.assigned_reviewer_id = #{reviewerId}</if>
            <if test="scope != null and scope == 'AVAILABLE'.toString()">
                AND s.assigned_reviewer_id IS NULL
                AND NOT EXISTS (SELECT 1 FROM review_task_claims rtc
                                WHERE rtc.task_id = s.task_id
                                  AND rtc.review_level = s.current_review_level)
            </if>
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
            @Param("reviewerId") Long reviewerId,
            @Param("scope") String scope,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(1)
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
            <if test="scope != null and scope == 'CLAIMED'.toString()"> AND s.assigned_reviewer_id = #{reviewerId}</if>
            <if test="scope != null and scope == 'AVAILABLE'.toString()">
                AND s.assigned_reviewer_id IS NULL
                AND NOT EXISTS (SELECT 1 FROM review_task_claims rtc
                                WHERE rtc.task_id = s.task_id
                                  AND rtc.review_level = s.current_review_level)
            </if>
            </script>
            """)
    long countWithFilters(
            @Param("taskId") Long taskId,
            @Param("submissionStatus") String submissionStatus,
            @Param("aiDecision") String aiDecision,
            @Param("aiReviewStatus") String aiReviewStatus,
            @Param("conflictStatus") String conflictStatus,
            @Param("reviewLevel") Integer reviewLevel,
            @Param("assignedReviewerId") Long assignedReviewerId,
            @Param("reviewerId") Long reviewerId,
            @Param("scope") String scope);

    @Select("""
            SELECT s.task_id AS taskId,
                   t.title AS taskTitle,
                   COUNT(1) AS pendingCount,
                   SUM(CASE WHEN s.assigned_reviewer_id = #{reviewerId} THEN 1 ELSE 0 END) AS myPendingCount,
                   0 AS totalReviewedCount,
                   MAX(CASE WHEN rtc.reviewer_id IS NOT NULL THEN 1 ELSE 0 END) AS claimed,
                   MAX(CASE WHEN rtc.reviewer_id = #{reviewerId} THEN 1 ELSE 0 END) AS claimedByMe
            FROM submissions s
            JOIN tasks t ON t.id = s.task_id
            LEFT JOIN review_task_claims rtc ON rtc.task_id = s.task_id
                   AND rtc.review_level = s.current_review_level
            WHERE s.status = 'PENDING_FINAL'
            GROUP BY s.task_id, t.title
            HAVING pendingCount > 0
            ORDER BY pendingCount DESC
            """)
    List<com.labelhub.modules.review.dto.ReviewerTaskSummary> selectTaskSummariesForReviewer(
            @Param("reviewerId") Long reviewerId);

    @Select("""
            SELECT s.id AS submissionId, s.task_id AS taskId,
                   t.title AS taskTitle,
                   s.status AS submissionStatus,
                   ar.status AS aiReviewStatus, ar.decision AS aiDecision,
                   CAST(ar.average_score AS CHAR) AS averageScore,
                   CASE WHEN s.assigned_reviewer_id = #{reviewerId} THEN TRUE ELSE FALSE END AS assignedToMe,
                   s.submitted_at AS submittedAt
            FROM submissions s
            JOIN tasks t ON t.id = s.task_id
            LEFT JOIN ai_review_results ar ON ar.submission_id = s.id
            WHERE s.status IN ('PENDING_FINAL', 'AI_REVIEWING')
              AND (s.assigned_reviewer_id = #{reviewerId}
                   OR s.id IN (
                     SELECT rt.submission_id FROM review_tasks rt
                     WHERE rt.assigned_reviewer_id = #{reviewerId}
                       AND rt.status IN ('PENDING', 'IN_REVIEW')
                   ))
            ORDER BY s.submitted_at DESC
            """)
    List<ReviewerAiReviewStatusItem> selectAiReviewStatusForReviewer(
            @Param("reviewerId") Long reviewerId);
}
