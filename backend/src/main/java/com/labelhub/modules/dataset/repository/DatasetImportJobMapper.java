package com.labelhub.modules.dataset.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.dataset.domain.DatasetImportJobEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DatasetImportJobMapper extends BaseMapper<DatasetImportJobEntity> {

    @Select("""
            select * from dataset_import_jobs
            where id = #{jobId} and task_id = #{taskId}
            """)
    DatasetImportJobEntity selectByTaskAndJob(@Param("taskId") Long taskId, @Param("jobId") Long jobId);
}
