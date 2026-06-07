package com.labelhub.modules.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.task.domain.TaskTag;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskTagMapper extends BaseMapper<TaskTag> {

    /**
     * 批量查询多个任务的标签，避免逐任务查询造成的 N+1。
     */
    @Select("""
            <script>
            SELECT task_id, tag_name
            FROM task_tags
            WHERE task_id IN
            <foreach item="id" collection="taskIds" open="(" separator="," close=")">
              #{id}
            </foreach>
            ORDER BY task_id, id
            </script>
            """)
    List<TaskTag> selectByTaskIds(@Param("taskIds") Collection<Long> taskIds);
}
