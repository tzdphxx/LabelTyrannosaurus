package com.labelhub.modules.review.mapper;

import com.labelhub.modules.review.dto.ReviewerTaskItemRow;
import com.labelhub.modules.review.dto.ReviewerTaskStatusCount;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReviewerTaskItemMapper {

    @Select("""
            SELECT COUNT(1)
            FROM task_reviewers tr
            WHERE tr.task_id = #{taskId}
              AND tr.reviewer_id = #{reviewerId}
            """)
    int countTaskReviewerAccess(@Param("taskId") Long taskId,
                                @Param("reviewerId") Long reviewerId);

    @Select("""
            SELECT COUNT(1)
            FROM submissions s
            WHERE s.task_id = #{taskId}
              AND s.assigned_reviewer_id = #{reviewerId}
              AND s.status != 'SUPERSEDED'
            """)
    int countSubmissionReviewerAccess(@Param("taskId") Long taskId,
                                      @Param("reviewerId") Long reviewerId);

    @Select("""
            SELECT COUNT(1)
            FROM review_tasks rt
            WHERE rt.task_id = #{taskId}
              AND rt.assigned_reviewer_id = #{reviewerId}
              AND rt.status != 'CANCELLED'
            """)
    int countReviewTaskAccess(@Param("taskId") Long taskId,
                              @Param("reviewerId") Long reviewerId);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM (
                SELECT di.id AS dataset_item_id,
                       di.external_id,
                       di.item_json,
                       di.metadata_json,
                       CASE
                           WHEN s.status = 'APPROVED' OR a.status = 'APPROVED' THEN 'APPROVED'
                           WHEN s.status = 'REJECTED' OR a.status IN ('RETURNED', 'AI_RETURNED') THEN 'RETURNED'
                           WHEN s.status IN ('SUBMITTED', 'AI_REVIEWING', 'PENDING_FINAL') OR a.status = 'SUBMITTED' THEN 'SUBMITTED'
                           WHEN a.status = 'DRAFTING' THEN 'DRAFT'
                           WHEN a.status = 'CLAIMED' THEN 'CLAIMED'
                           ELSE 'UNCLAIMED'
                       END AS item_status,
                       s.status AS submission_status,
                       ar.decision AS ai_decision
                FROM dataset_items di
                LEFT JOIN assignments a ON a.id = (
                    SELECT a2.id
                    FROM assignments a2
                    WHERE a2.dataset_item_id = di.id
                      AND a2.status != 'CANCELLED'
                    ORDER BY a2.updated_at DESC, a2.id DESC
                    LIMIT 1
                )
                LEFT JOIN submissions s ON s.id = (
                    SELECT s2.id
                    FROM submissions s2
                    WHERE s2.assignment_id = a.id
                      AND s2.status != 'SUPERSEDED'
                    ORDER BY s2.version_no DESC, s2.id DESC
                    LIMIT 1
                )
                LEFT JOIN ai_review_results ar ON ar.submission_id = s.id
                WHERE di.task_id = #{taskId}
                  AND di.deleted = 0
            ) base
            WHERE 1 = 1
            <if test="itemStatus != null"> AND base.item_status = #{itemStatus}</if>
            <if test="submissionStatus != null"> AND base.submission_status = #{submissionStatus}</if>
            <if test="aiDecision != null"> AND base.ai_decision = #{aiDecision}</if>
            <if test="keyword != null">
              AND (
                base.external_id LIKE CONCAT('%', #{keyword}, '%')
                OR base.item_json LIKE CONCAT('%', #{keyword}, '%')
                OR base.metadata_json LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            </script>
            """)
    long countTaskItems(@Param("taskId") Long taskId,
                        @Param("itemStatus") String itemStatus,
                        @Param("submissionStatus") String submissionStatus,
                        @Param("aiDecision") String aiDecision,
                        @Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT base.dataset_item_id AS datasetItemId,
                   base.external_id AS externalId,
                   base.item_json AS itemJson,
                   base.metadata_json AS metadataJson,
                   base.item_status AS itemStatus,
                   base.assignment_id AS assignmentId,
                   base.assignment_status AS assignmentStatus,
                   base.labeler_id AS labelerId,
                   base.labeler_name AS labelerName,
                   base.latest_submission_id AS latestSubmissionId,
                   base.version_no AS versionNo,
                   base.submission_status AS submissionStatus,
                   base.submitted_at AS submittedAt,
                   base.ai_review_status AS aiReviewStatus,
                   base.ai_decision AS aiDecision,
                   base.average_score AS averageScore,
                   base.risk_flags AS riskFlags,
                   base.suggestion AS suggestion,
                   base.review_task_status AS reviewTaskStatus,
                   base.review_level AS reviewLevel,
                   base.latest_review_action AS latestReviewAction,
                   base.latest_review_at AS latestReviewAt,
                   CASE WHEN base.latest_submission_id IS NULL THEN FALSE ELSE TRUE END AS canOpenSubmissionDetail,
                   CASE WHEN base.submission_status = 'PENDING_FINAL' THEN TRUE ELSE FALSE END AS canReview
            FROM (
                SELECT di.id AS dataset_item_id,
                       di.external_id,
                       di.item_json,
                       di.metadata_json,
                       CASE
                           WHEN s.status = 'APPROVED' OR a.status = 'APPROVED' THEN 'APPROVED'
                           WHEN s.status = 'REJECTED' OR a.status IN ('RETURNED', 'AI_RETURNED') THEN 'RETURNED'
                           WHEN s.status IN ('SUBMITTED', 'AI_REVIEWING', 'PENDING_FINAL') OR a.status = 'SUBMITTED' THEN 'SUBMITTED'
                           WHEN a.status = 'DRAFTING' THEN 'DRAFT'
                           WHEN a.status = 'CLAIMED' THEN 'CLAIMED'
                           ELSE 'UNCLAIMED'
                       END AS item_status,
                       a.id AS assignment_id,
                       a.status AS assignment_status,
                       a.labeler_id,
                       COALESCE(u.display_name, u.username) AS labeler_name,
                       s.id AS latest_submission_id,
                       s.version_no,
                       s.status AS submission_status,
                       s.submitted_at,
                       ar.status AS ai_review_status,
                       ar.decision AS ai_decision,
                       CAST(ar.average_score AS CHAR) AS average_score,
                       ar.risk_flags,
                       ar.suggestion,
                       rt.status AS review_task_status,
                       rt.review_level,
                       rr.action AS latest_review_action,
                       rr.created_at AS latest_review_at
                FROM dataset_items di
                LEFT JOIN assignments a ON a.id = (
                    SELECT a2.id
                    FROM assignments a2
                    WHERE a2.dataset_item_id = di.id
                      AND a2.status != 'CANCELLED'
                    ORDER BY a2.updated_at DESC, a2.id DESC
                    LIMIT 1
                )
                LEFT JOIN users u ON u.id = a.labeler_id
                LEFT JOIN submissions s ON s.id = (
                    SELECT s2.id
                    FROM submissions s2
                    WHERE s2.assignment_id = a.id
                      AND s2.status != 'SUPERSEDED'
                    ORDER BY s2.version_no DESC, s2.id DESC
                    LIMIT 1
                )
                LEFT JOIN ai_review_results ar ON ar.submission_id = s.id
                LEFT JOIN review_tasks rt ON rt.id = (
                    SELECT rt2.id
                    FROM review_tasks rt2
                    WHERE rt2.submission_id = s.id
                      AND rt2.assigned_reviewer_id = #{reviewerId}
                      AND rt2.status != 'CANCELLED'
                    ORDER BY rt2.review_level DESC, rt2.id DESC
                    LIMIT 1
                )
                LEFT JOIN review_records rr ON rr.id = (
                    SELECT rr2.id
                    FROM review_records rr2
                    WHERE rr2.submission_id = s.id
                    ORDER BY rr2.created_at DESC, rr2.id DESC
                    LIMIT 1
                )
                WHERE di.task_id = #{taskId}
                  AND di.deleted = 0
            ) base
            WHERE 1 = 1
            <if test="itemStatus != null"> AND base.item_status = #{itemStatus}</if>
            <if test="submissionStatus != null"> AND base.submission_status = #{submissionStatus}</if>
            <if test="aiDecision != null"> AND base.ai_decision = #{aiDecision}</if>
            <if test="keyword != null">
              AND (
                base.external_id LIKE CONCAT('%', #{keyword}, '%')
                OR base.item_json LIKE CONCAT('%', #{keyword}, '%')
                OR base.metadata_json LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            ORDER BY base.dataset_item_id ASC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ReviewerTaskItemRow> selectTaskItems(
            @Param("taskId") Long taskId,
            @Param("reviewerId") Long reviewerId,
            @Param("itemStatus") String itemStatus,
            @Param("submissionStatus") String submissionStatus,
            @Param("aiDecision") String aiDecision,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            SELECT item_status AS itemStatus, COUNT(1) AS count
            FROM (
                SELECT CASE
                           WHEN s.status = 'APPROVED' OR a.status = 'APPROVED' THEN 'APPROVED'
                           WHEN s.status = 'REJECTED' OR a.status IN ('RETURNED', 'AI_RETURNED') THEN 'RETURNED'
                           WHEN s.status IN ('SUBMITTED', 'AI_REVIEWING', 'PENDING_FINAL') OR a.status = 'SUBMITTED' THEN 'SUBMITTED'
                           WHEN a.status = 'DRAFTING' THEN 'DRAFT'
                           WHEN a.status = 'CLAIMED' THEN 'CLAIMED'
                           ELSE 'UNCLAIMED'
                       END AS item_status
                FROM dataset_items di
                LEFT JOIN assignments a ON a.id = (
                    SELECT a2.id
                    FROM assignments a2
                    WHERE a2.dataset_item_id = di.id
                      AND a2.status != 'CANCELLED'
                    ORDER BY a2.updated_at DESC, a2.id DESC
                    LIMIT 1
                )
                LEFT JOIN submissions s ON s.id = (
                    SELECT s2.id
                    FROM submissions s2
                    WHERE s2.assignment_id = a.id
                      AND s2.status != 'SUPERSEDED'
                    ORDER BY s2.version_no DESC, s2.id DESC
                    LIMIT 1
                )
                WHERE di.task_id = #{taskId}
                  AND di.deleted = 0
            ) base
            GROUP BY item_status
            """)
    List<ReviewerTaskStatusCount> selectStatusCounts(@Param("taskId") Long taskId);
}
