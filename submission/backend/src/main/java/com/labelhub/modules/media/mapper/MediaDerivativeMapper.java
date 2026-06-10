package com.labelhub.modules.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.media.domain.MediaDerivativeEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MediaDerivativeMapper extends BaseMapper<MediaDerivativeEntity> {

    @Select("""
            SELECT d.* FROM media_derivatives d
            JOIN media_assets a ON a.id = d.asset_id
            WHERE a.dataset_item_id = #{datasetItemId}
            ORDER BY d.asset_id, d.sequence_no, d.id
            """)
    List<MediaDerivativeEntity> selectByDatasetItemId(@Param("datasetItemId") Long datasetItemId);

    @Delete("""
            DELETE d FROM media_derivatives d
            JOIN media_assets a ON a.id = d.asset_id
            WHERE a.dataset_item_id = #{datasetItemId}
            """)
    int deleteByDatasetItemId(@Param("datasetItemId") Long datasetItemId);
}
