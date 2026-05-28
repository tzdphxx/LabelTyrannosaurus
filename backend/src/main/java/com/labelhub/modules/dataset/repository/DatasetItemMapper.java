package com.labelhub.modules.dataset.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.dataset.domain.DatasetItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DatasetItemMapper extends BaseMapper<DatasetItemEntity> {

    @Select("""
            select count(1) from dataset_items
            where task_id = #{taskId}
              and external_id = #{externalId}
              and deleted = 0
            """)
    int countActiveByTaskIdAndExternalId(@Param("taskId") Long taskId, @Param("externalId") String externalId);

    @Update("""
            update dataset_items
            set deleted = 1
            where task_id = #{taskId}
              and deleted = 0
            """)
    int softDeleteActiveByTaskId(@Param("taskId") Long taskId);
}
