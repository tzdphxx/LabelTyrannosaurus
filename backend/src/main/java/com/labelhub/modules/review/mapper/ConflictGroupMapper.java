package com.labelhub.modules.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.review.domain.ConflictGroup;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConflictGroupMapper extends BaseMapper<ConflictGroup> {

    @Select("""
            SELECT *
            FROM conflict_groups
            WHERE task_id = #{taskId}
              AND dataset_item_id = #{datasetItemId}
            LIMIT 1
            """)
    ConflictGroup selectByTaskAndItem(@Param("taskId") Long taskId,
                                      @Param("datasetItemId") Long datasetItemId);

    @Select("""
            SELECT *
            FROM conflict_groups
            WHERE status = 'OPEN'
            ORDER BY created_at ASC
            """)
    List<ConflictGroup> selectOpenGroups();
}
