package com.labelhub.modules.dataset.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.dataset.domain.DatasetItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 数据集题目表 Mapper。
 */
@Mapper
public interface DatasetItemMapper extends BaseMapper<DatasetItemEntity> {

    /**
     * 查询同一任务下未删除题目的 externalId 是否已存在。
     */
    @Select("""
            select count(1) from dataset_items
            where task_id = #{taskId}
              and external_id = #{externalId}
              and deleted = 0
    """)
    int countActiveByTaskIdAndExternalId(@Param("taskId") Long taskId, @Param("externalId") String externalId);

    /**
     * 覆盖导入前软删除任务下现有题目，保留历史记录用于审计和回溯。
     */
    @Update("""
            update dataset_items
            set deleted = 1
            where task_id = #{taskId}
              and deleted = 0
            """)
    int softDeleteActiveByTaskId(@Param("taskId") Long taskId);
}
