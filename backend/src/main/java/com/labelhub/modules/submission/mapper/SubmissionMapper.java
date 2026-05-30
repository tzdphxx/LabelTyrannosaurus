package com.labelhub.modules.submission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.submission.domain.Submission;
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
                           'AI_REVIEWING', 'SUBMITTED', 'AI_REJECTED'),
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
}
