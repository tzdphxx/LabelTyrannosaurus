package com.labelhub.modules.assignment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AssignmentMapper extends BaseMapper<Assignment> {

    @Select("""
            SELECT COUNT(1)
            FROM assignments
            WHERE task_id = #{taskId}
              AND labeler_id = #{labelerId}
            """)
    Integer countByTaskAndLabeler(@Param("taskId") Long taskId, @Param("labelerId") Long labelerId);

    @Select("""
            SELECT *
            FROM assignments
            WHERE id = #{assignmentId}
              AND labeler_id = #{labelerId}
            """)
    Assignment selectOwnedAssignment(@Param("assignmentId") Long assignmentId,
                                     @Param("labelerId") Long labelerId);

    @Update("""
            UPDATE assignments
            SET draft_answer_json = #{answerJson},
                draft_version = #{nextDraftVersion},
                status = #{nextStatus},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{assignmentId}
              AND labeler_id = #{labelerId}
              AND draft_version = #{expectedDraftVersion}
              AND status IN ('CLAIMED', 'DRAFTING', 'RETURNED')
            """)
    int updateDraftIfVersionMatches(@Param("assignmentId") Long assignmentId,
                                    @Param("labelerId") Long labelerId,
                                    @Param("answerJson") String answerJson,
                                    @Param("expectedDraftVersion") Integer expectedDraftVersion,
                                    @Param("nextDraftVersion") Integer nextDraftVersion,
                                    @Param("nextStatus") AssignmentStatus nextStatus);

    @Update("""
            UPDATE assignments
            SET status = #{nextStatus},
                submitted_at = CURRENT_TIMESTAMP(3),
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{assignmentId}
              AND labeler_id = #{labelerId}
              AND draft_version = #{expectedDraftVersion}
              AND status IN ('CLAIMED', 'DRAFTING', 'RETURNED')
            """)
    int markSubmittedIfCurrent(@Param("assignmentId") Long assignmentId,
                               @Param("labelerId") Long labelerId,
                               @Param("expectedDraftVersion") Integer expectedDraftVersion,
                               @Param("nextStatus") AssignmentStatus nextStatus);

    @Select("""
            <script>
            SELECT a.*, t.title AS task_title
            FROM assignments a
            INNER JOIN tasks t ON t.id = a.task_id
            WHERE a.labeler_id = #{labelerId}
            <if test="taskId != null">
              AND a.task_id = #{taskId}
            </if>
            <if test="status != null">
              AND a.status = #{status}
            </if>
            ORDER BY a.updated_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    java.util.List<java.util.Map<String, Object>> selectLabelerAssignments(
            @Param("labelerId") Long labelerId,
            @Param("taskId") Long taskId,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM assignments a
            WHERE a.labeler_id = #{labelerId}
            <if test="taskId != null">
              AND a.task_id = #{taskId}
            </if>
            <if test="status != null">
              AND a.status = #{status}
            </if>
            </script>
            """)
    long countLabelerAssignments(@Param("labelerId") Long labelerId,
                                 @Param("taskId") Long taskId,
                                 @Param("status") String status);

    @Update("""
            UPDATE assignments
            SET status = 'CANCELLED',
                cancelled_at = CURRENT_TIMESTAMP(3),
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{assignmentId}
              AND labeler_id = #{labelerId}
              AND status IN ('CLAIMED', 'DRAFTING', 'RETURNED')
            """)
    int markCancelled(@Param("assignmentId") Long assignmentId,
                      @Param("labelerId") Long labelerId);
}
