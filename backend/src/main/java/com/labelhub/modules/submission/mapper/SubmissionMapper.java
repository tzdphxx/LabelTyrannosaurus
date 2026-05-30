package com.labelhub.modules.submission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.submission.domain.Submission;
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
    java.util.List<Submission> selectPendingFinalByTaskAndItem(@Param("taskId") Long taskId,
                                                               @Param("datasetItemId") Long datasetItemId);

    @Select("""
            SELECT *
            FROM submissions
            WHERE labeler_id = #{labelerId}
              AND task_id = #{taskId}
            ORDER BY created_at DESC
            LIMIT #{limit}
            """)
    java.util.List<Submission> selectRecentByLabeler(@Param("labelerId") Long labelerId,
                                                     @Param("taskId") Long taskId,
                                                     @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM submissions
            WHERE assignment_id = #{assignmentId}
            ORDER BY version_no ASC
            """)
    java.util.List<Submission> selectByAssignmentId(@Param("assignmentId") Long assignmentId);
}
