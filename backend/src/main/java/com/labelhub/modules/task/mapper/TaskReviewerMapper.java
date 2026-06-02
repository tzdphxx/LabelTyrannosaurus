package com.labelhub.modules.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.task.domain.TaskReviewer;
import com.labelhub.modules.task.dto.TaskReviewerResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskReviewerMapper extends BaseMapper<TaskReviewer> {

    @Select("""
            SELECT reviewer_id
            FROM task_reviewers
            WHERE task_id = #{taskId}
            """)
    List<Long> selectReviewerIdsByTask(@Param("taskId") Long taskId);

    @Select("""
            SELECT tr.reviewer_id, u.username, u.display_name, tr.created_at AS assigned_at
            FROM task_reviewers tr
            INNER JOIN users u ON u.id = tr.reviewer_id
            WHERE tr.task_id = #{taskId}
            ORDER BY tr.created_at ASC
            """)
    List<TaskReviewerResponse> selectReviewerDetails(@Param("taskId") Long taskId);
}
