package com.labelhub.modules.dataset.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.dataset.domain.DatasetFileEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据集源文件表 Mapper。
 */
@Mapper
public interface DatasetFileMapper extends BaseMapper<DatasetFileEntity> {
}
