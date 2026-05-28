package com.labelhub.modules.dataset.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.dataset.domain.DatasetItemChangeLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DatasetItemChangeLogMapper extends BaseMapper<DatasetItemChangeLogEntity> {
}
