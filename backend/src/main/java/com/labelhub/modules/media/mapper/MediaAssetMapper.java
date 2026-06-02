package com.labelhub.modules.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.media.domain.MediaAssetEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MediaAssetMapper extends BaseMapper<MediaAssetEntity> {

    @Select("""
            SELECT * FROM media_assets
            WHERE dataset_item_id = #{datasetItemId}
            ORDER BY id
            """)
    List<MediaAssetEntity> selectByDatasetItemId(@Param("datasetItemId") Long datasetItemId);

    @Delete("DELETE FROM media_assets WHERE dataset_item_id = #{datasetItemId}")
    int deleteByDatasetItemId(@Param("datasetItemId") Long datasetItemId);
}
