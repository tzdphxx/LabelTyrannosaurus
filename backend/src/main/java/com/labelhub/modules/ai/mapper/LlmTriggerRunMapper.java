package com.labelhub.modules.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.ai.domain.LlmTriggerRun;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LlmTriggerRunMapper extends BaseMapper<LlmTriggerRun> {

    @Select("""
            <script>
            SELECT *
            FROM llm_trigger_runs
            WHERE task_id = #{query.taskId}
            <if test="query.status != null and query.status != ''">
              AND status = #{query.status}
            </if>
            <if test="query.componentId != null and query.componentId != ''">
              AND component_id = #{query.componentId}
            </if>
            <if test="query.startTime != null">
              AND created_at >= #{query.startTime}
            </if>
            <if test="query.endTime != null">
              AND created_at &lt;= #{query.endTime}
            </if>
            ORDER BY created_at DESC
            LIMIT #{query.normalizedPageSize} OFFSET #{query.offset}
            </script>
            """)
    List<LlmTriggerRun> selectPageByTaskId(@Param("query") com.labelhub.modules.ai.dto.LlmTriggerRunQuery query);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM llm_trigger_runs
            WHERE task_id = #{query.taskId}
            <if test="query.status != null and query.status != ''">
              AND status = #{query.status}
            </if>
            <if test="query.componentId != null and query.componentId != ''">
              AND component_id = #{query.componentId}
            </if>
            <if test="query.startTime != null">
              AND created_at >= #{query.startTime}
            </if>
            <if test="query.endTime != null">
              AND created_at &lt;= #{query.endTime}
            </if>
            </script>
            """)
    long countByTaskId(@Param("query") com.labelhub.modules.ai.dto.LlmTriggerRunQuery query);
}
