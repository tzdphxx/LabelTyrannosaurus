package com.labelhub.modules.task.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.task.domain.TaskEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务表只读 Mapper。
 *
 * <p>BE-B 在 Task5 中仅读取任务归属和状态，不推进任务状态机。</p>
 */
@Mapper
public interface TaskMapper extends BaseMapper<TaskEntity> {
}
