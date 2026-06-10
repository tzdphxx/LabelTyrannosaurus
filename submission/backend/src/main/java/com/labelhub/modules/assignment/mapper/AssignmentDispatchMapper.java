package com.labelhub.modules.assignment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.assignment.domain.AssignmentDispatch;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AssignmentDispatchMapper extends BaseMapper<AssignmentDispatch> {

    @Select("""
            SELECT *
            FROM assignment_dispatches
            WHERE task_id = #{taskId}
              AND labeler_id = #{labelerId}
              AND status = 'PENDING'
            ORDER BY id ASC
            LIMIT 1
            """)
    AssignmentDispatch selectPendingForLabeler(@Param("taskId") Long taskId,
                                                @Param("labelerId") Long labelerId);

    @Update("""
            UPDATE assignment_dispatches
            SET status = 'CLAIMED',
                claimed_at = CURRENT_TIMESTAMP(3),
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{dispatchId}
              AND status = 'PENDING'
            """)
    int claimById(@Param("dispatchId") Long dispatchId);

    @Update("""
            UPDATE assignment_dispatches
            SET status = 'REVOKED',
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{dispatchId}
              AND status = 'PENDING'
            """)
    int revokeById(@Param("dispatchId") Long dispatchId);

    @Select("""
            SELECT *
            FROM assignment_dispatches
            WHERE task_id = #{taskId}
            ORDER BY dispatched_at ASC
            """)
    List<AssignmentDispatch> selectByTask(@Param("taskId") Long taskId);

    @Select("""
            SELECT COUNT(1)
            FROM assignment_dispatches
            WHERE task_id = #{taskId}
            """)
    int countByTaskId(@Param("taskId") Long taskId);

    @Select("""
            SELECT *
            FROM assignment_dispatches
            WHERE task_id = #{taskId}
              AND labeler_id = #{labelerId}
              AND status IN ('PENDING', 'CLAIMED')
            ORDER BY status ASC, dispatched_at ASC
            """)
    List<AssignmentDispatch> selectForLabeler(@Param("taskId") Long taskId,
                                               @Param("labelerId") Long labelerId);

    @Select("""
            SELECT COUNT(1)
            FROM assignment_dispatches
            WHERE task_id = #{taskId}
              AND dataset_item_id = #{datasetItemId}
              AND status = 'PENDING'
            """)
    int countPendingByItem(@Param("taskId") Long taskId,
                           @Param("datasetItemId") Long datasetItemId);

    @Update("""
            UPDATE tasks t
            SET t.quota = (SELECT COUNT(1) FROM assignment_dispatches WHERE task_id = #{taskId}),
                t.updated_at = CURRENT_TIMESTAMP(3)
            WHERE t.id = #{taskId}
              AND t.strategy = 'ASSIGNED'
            """)
    int syncQuotaToTask(@Param("taskId") Long taskId);
}
