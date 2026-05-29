package com.labelhub.modules.export.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.export.domain.ExportJobEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 导出任务 Mapper。
 */
@Mapper
public interface ExportJobMapper extends BaseMapper<ExportJobEntity> {

    @Select("""
            select * from export_jobs
            where task_id = #{taskId}
              and id = #{jobId}
            """)
    ExportJobEntity selectByTaskAndJob(@Param("taskId") Long taskId, @Param("jobId") Long jobId);

    @Select("""
            <script>
            select * from export_jobs
            where task_id = #{taskId}
            order by created_at desc, id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<ExportJobEntity> selectPageByTask(@Param("taskId") Long taskId,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset);

    @Select("""
            select count(1) from export_jobs
            where task_id = #{taskId}
            """)
    long countByTask(@Param("taskId") Long taskId);
}
