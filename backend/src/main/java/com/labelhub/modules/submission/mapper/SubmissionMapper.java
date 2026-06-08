package com.labelhub.modules.submission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.submission.domain.Submission;
import java.util.Map;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {

    @Select("""
            SELECT *
            FROM submissions
            WHERE assignment_id = #{assignmentId}
            ORDER BY version_no DESC
            LIMIT 1
            """)
    Submission selectLatestByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Select("""
            SELECT *
            FROM submissions
            WHERE assignment_id = #{assignmentId}
              AND status <> 'SUPERSEDED'
            ORDER BY version_no DESC
            LIMIT 1
            """)
    Submission selectLatestActiveByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Update("""
            UPDATE submissions
            SET status = 'SUPERSEDED',
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE assignment_id = #{assignmentId}
              AND status <> 'SUPERSEDED'
            """)
    int supersedeActiveByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Select("""
            SELECT COUNT(1)
            FROM submissions
            WHERE task_id = #{taskId}
              AND dataset_item_id = #{datasetItemId}
              AND status = 'PENDING_FINAL'
            """)
    int countPendingFinalByTaskAndItem(@Param("taskId") Long taskId,
                                       @Param("datasetItemId") Long datasetItemId);

    @Select("""
            SELECT *
            FROM submissions
            WHERE task_id = #{taskId}
              AND dataset_item_id = #{datasetItemId}
              AND status = 'PENDING_FINAL'
            """)
    List<Submission> selectPendingFinalByTaskAndItem(@Param("taskId") Long taskId,
                                                     @Param("datasetItemId") Long datasetItemId);

    @Select("""
            SELECT *
            FROM submissions
            WHERE task_id = #{taskId}
              AND dataset_item_id = #{datasetItemId}
              AND status <> 'SUPERSEDED'
            ORDER BY FIELD(status, 'PENDING_FINAL', 'APPROVED', 'REJECTED',
                           'AI_REVIEWING', 'SUBMITTED'),
                     version_no DESC,
                     id ASC
            """)
    List<Submission> selectConflictCandidates(@Param("taskId") Long taskId,
                                              @Param("datasetItemId") Long datasetItemId);

    @Update("""
            UPDATE submissions
            SET is_golden = 0,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE dataset_item_id = #{datasetItemId}
              AND is_golden = 1
            """)
    int clearGoldenByDatasetItem(@Param("datasetItemId") Long datasetItemId);

    @Select("""
            SELECT *
            FROM submissions
            WHERE labeler_id = #{labelerId}
              AND task_id = #{taskId}
            ORDER BY created_at DESC
            LIMIT #{limit}
            """)
    List<Submission> selectRecentByLabeler(@Param("labelerId") Long labelerId,
                                           @Param("taskId") Long taskId,
                                           @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM submissions
            WHERE assignment_id = #{assignmentId}
            ORDER BY version_no ASC
            """)
    List<Submission> selectByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Select("""
            <script>
            SELECT *
            FROM submissions
            WHERE task_id = #{taskId}
              AND dataset_item_id = #{datasetItemId}
            <if test="labelerId != null">
              AND labeler_id = #{labelerId}
            </if>
            ORDER BY submitted_at ASC, id ASC
            </script>
            """)
    List<Submission> selectItemHistorySubmissions(@Param("taskId") Long taskId,
                                                  @Param("datasetItemId") Long datasetItemId,
                                                  @Param("labelerId") Long labelerId);

    @Update("""
            UPDATE submissions
            SET status = #{newStatus},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{submissionId}
              AND status = #{expectedStatus}
            """)
    int casUpdateStatus(@Param("submissionId") Long submissionId,
                        @Param("expectedStatus") String expectedStatus,
                        @Param("newStatus") String newStatus);

    @Update("""
            UPDATE submissions
            SET status = 'APPROVED',
                is_golden = 1,
                review_flow_status = 'FINAL_APPROVED',
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{submissionId}
              AND status = 'PENDING_FINAL'
            """)
    int markApprovedIfPendingFinal(@Param("submissionId") Long submissionId);

    @Update("""
            UPDATE submissions
            SET status = 'REJECTED',
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{submissionId}
              AND status = 'PENDING_FINAL'
            """)
    int markRejectedIfPendingFinal(@Param("submissionId") Long submissionId);

    @Update("""
            UPDATE submissions
            SET status = 'REJECTED',
                is_golden = 0,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{submissionId}
              AND status = 'PENDING_FINAL'
            """)
    int markConflictRejectedIfPendingFinal(@Param("submissionId") Long submissionId);

    @Select("""
            SELECT status, COUNT(1) AS count
            FROM submissions
            WHERE task_id = #{taskId}
              AND status <> 'SUPERSEDED'
            GROUP BY status
            """)
    List<Map<String, Object>> selectStatusCountsByTaskId(@Param("taskId") Long taskId);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM submissions s
            <if test="assignmentStatus != null">
            INNER JOIN assignments a ON a.id = s.assignment_id
            </if>
            WHERE s.status != 'SUPERSEDED'
            <if test="!includeAllLabelers"> AND s.labeler_id = #{labelerId}</if>
            <if test="taskId != null"> AND s.task_id = #{taskId}</if>
            <if test="submissionStatus != null"> AND s.status = #{submissionStatus}</if>
            <if test="assignmentStatus != null"> AND a.status = #{assignmentStatus}</if>
            </script>
            """)
    long countLabelerSubmissions(@Param("labelerId") Long labelerId,
                                 @Param("taskId") Long taskId,
                                 @Param("submissionStatus") String submissionStatus,
                                 @Param("assignmentStatus") String assignmentStatus,
                                 @Param("includeAllLabelers") boolean includeAllLabelers);

    @Select("""
            <script>
            SELECT s.*
            FROM submissions s
            <if test="assignmentStatus != null">
            INNER JOIN assignments a ON a.id = s.assignment_id
            </if>
            WHERE s.status != 'SUPERSEDED'
            <if test="!includeAllLabelers"> AND s.labeler_id = #{labelerId}</if>
            <if test="taskId != null"> AND s.task_id = #{taskId}</if>
            <if test="submissionStatus != null"> AND s.status = #{submissionStatus}</if>
            <if test="assignmentStatus != null"> AND a.status = #{assignmentStatus}</if>
            ORDER BY s.submitted_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Submission> selectLabelerSubmissionsPage(@Param("labelerId") Long labelerId,
                                                  @Param("taskId") Long taskId,
                                                  @Param("submissionStatus") String submissionStatus,
                                                  @Param("assignmentStatus") String assignmentStatus,
                                                  @Param("includeAllLabelers") boolean includeAllLabelers,
                                                  @Param("limit") int limit,
                                                  @Param("offset") int offset);

    @Select("""
            SELECT id
            FROM submissions
            WHERE status = 'PENDING_FINAL'
              AND assigned_reviewer_id IS NULL
            ORDER BY submitted_at ASC
            LIMIT #{limit}
            FOR UPDATE SKIP LOCKED
            """)
    List<Long> selectUnassignedPendingFinal(@Param("limit") int limit);

    @Select("""
            <script>
            SELECT id
            FROM submissions
            WHERE status = 'PENDING_FINAL'
              AND assigned_reviewer_id IS NULL
              <if test="taskId != null"> AND task_id = #{taskId}</if>
            ORDER BY submitted_at ASC
            LIMIT #{limit}
            FOR UPDATE SKIP LOCKED
            </script>
            """)
    List<Long> selectUnassignedPendingFinalByTask(@Param("taskId") Long taskId,
                                                   @Param("limit") int limit);

    @Update("""
            UPDATE submissions
            SET assigned_reviewer_id = #{reviewerId},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{submissionId}
              AND assigned_reviewer_id IS NULL
            """)
    int assignReviewer(@Param("submissionId") Long submissionId,
                       @Param("reviewerId") Long reviewerId);

    @Select("""
            SELECT COUNT(1)
            FROM submissions
            WHERE assigned_reviewer_id = #{reviewerId}
              AND status = 'PENDING_FINAL'
            """)
    int countPendingByReviewer(@Param("reviewerId") Long reviewerId);

    @Select("""
            SELECT COUNT(1)
            FROM submissions
            WHERE task_id = #{taskId}
              AND status = #{status}
              AND status <> 'SUPERSEDED'
            """)
    int countByTaskIdAndStatus(@Param("taskId") Long taskId,
                               @Param("status") String status);

    @Update("""
            UPDATE submissions
            SET assigned_reviewer_id = #{reviewerId},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE task_id = #{taskId}
              AND current_review_level = #{reviewLevel}
              AND status = 'PENDING_FINAL'
              AND assigned_reviewer_id IS NULL
            """)
    int assignReviewerForTaskLevel(@Param("taskId") Long taskId,
                                   @Param("reviewLevel") Integer reviewLevel,
                                   @Param("reviewerId") Long reviewerId);

    @Update("""
            UPDATE submissions
            SET assigned_reviewer_id = NULL,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE task_id = #{taskId}
              AND current_review_level = #{reviewLevel}
              AND status = 'PENDING_FINAL'
              AND assigned_reviewer_id = #{reviewerId}
            """)
    int clearReviewerForTaskLevel(@Param("taskId") Long taskId,
                                  @Param("reviewLevel") Integer reviewLevel,
                                  @Param("reviewerId") Long reviewerId);
}
