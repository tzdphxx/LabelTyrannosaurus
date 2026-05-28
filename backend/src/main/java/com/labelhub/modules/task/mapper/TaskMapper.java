package com.labelhub.modules.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.task.domain.Task;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    @Select("""
            <script>
            SELECT t.*
            FROM tasks t
            WHERE t.status = 'PUBLISHED'
              AND t.deadline_at &gt; #{now}
              <if test="status != null">
                AND t.status = #{status}
              </if>
              <if test="keyword != null">
                AND (
                  t.title LIKE CONCAT('%', #{keyword}, '%')
                  OR t.description LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
              <if test="tag != null">
                AND EXISTS (
                  SELECT 1
                  FROM task_tags tt
                  WHERE tt.task_id = t.id
                    AND tt.tag_name = #{tag}
                )
              </if>
            ORDER BY t.published_at DESC, t.id DESC
            </script>
            """)
    List<Task> selectPublishedMarketTasks(@Param("keyword") String keyword,
                                          @Param("tag") String tag,
                                          @Param("status") String status,
                                          @Param("now") LocalDateTime now);
}
