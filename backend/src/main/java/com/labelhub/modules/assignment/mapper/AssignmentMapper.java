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
}
