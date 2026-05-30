package com.labelhub.modules.preannotation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.preannotation.domain.PreAnnotation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PreAnnotationMapper extends BaseMapper<PreAnnotation> {

    @Select("""
            SELECT *
            FROM pre_annotations
            WHERE assignment_id = #{assignmentId}
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """)
    PreAnnotation selectLatestByAssignmentId(@Param("assignmentId") Long assignmentId);
}
