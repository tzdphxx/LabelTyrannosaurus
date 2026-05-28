package com.labelhub.modules.assignment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.assignment.domain.Assignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AssignmentMapper extends BaseMapper<Assignment> {

    @Select("""
            SELECT COUNT(1)
            FROM assignments
            WHERE task_id = #{taskId}
              AND labeler_id = #{labelerId}
            """)
    Integer countByTaskAndLabeler(@Param("taskId") Long taskId, @Param("labelerId") Long labelerId);
}
