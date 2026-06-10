package com.labelhub.modules.dataset.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.dataset.domain.DatasetItemChangeLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据集题目变更日志 Mapper。
 */
@Mapper
public interface DatasetItemChangeLogMapper extends BaseMapper<DatasetItemChangeLogEntity> {
}
