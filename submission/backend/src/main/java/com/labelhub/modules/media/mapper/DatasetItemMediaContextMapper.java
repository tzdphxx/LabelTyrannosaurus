package com.labelhub.modules.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.media.domain.DatasetItemMediaContextEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DatasetItemMediaContextMapper extends BaseMapper<DatasetItemMediaContextEntity> {

    @Select("""
            SELECT * FROM dataset_item_media_contexts
            WHERE dataset_item_id = #{datasetItemId}
            ORDER BY id DESC
            LIMIT 1
            """)
    DatasetItemMediaContextEntity selectLatestByDatasetItemId(@Param("datasetItemId") Long datasetItemId);
}
