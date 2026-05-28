package com.labelhub.modules.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DatasetItemMapper extends BaseMapper<DatasetItem> {

    @Select("""
            SELECT di.id
            FROM dataset_items di
            WHERE di.task_id = #{taskId}
              AND di.deleted = 0
              AND di.assigned_count < #{overlapCount}
              AND NOT EXISTS (
                SELECT 1
                FROM assignments a
                WHERE a.dataset_item_id = di.id
                  AND a.labeler_id = #{labelerId}
              )
            ORDER BY di.id
            LIMIT 1
            """)
    Long selectClaimableItemId(@Param("taskId") Long taskId,
                               @Param("labelerId") Long labelerId,
                               @Param("overlapCount") Integer overlapCount);

    @Update("""
            UPDATE dataset_items
            SET assigned_count = assigned_count + 1
            WHERE id = #{datasetItemId}
              AND deleted = 0
              AND assigned_count < #{overlapCount}
            """)
    int reserveIfAvailable(@Param("datasetItemId") Long datasetItemId,
                           @Param("overlapCount") Integer overlapCount);
}
