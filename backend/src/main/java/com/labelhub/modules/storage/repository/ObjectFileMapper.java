package com.labelhub.modules.storage.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.storage.domain.ObjectFileEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ObjectFileMapper extends BaseMapper<ObjectFileEntity> {
}
