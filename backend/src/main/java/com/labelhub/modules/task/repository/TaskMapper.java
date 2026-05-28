package com.labelhub.modules.task.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.task.domain.TaskEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<TaskEntity> {
}
