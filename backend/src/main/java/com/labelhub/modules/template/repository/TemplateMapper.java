package com.labelhub.modules.template.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.template.domain.TemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 模板主表 Mapper。
 */
@Mapper
public interface TemplateMapper extends BaseMapper<TemplateEntity> {

    /**
     * 查询任务下的模板列表，供 Owner 模板管理页展示。
     */
    @Select("""
            select * from templates
            where task_id = #{taskId}
            order by id asc
            """)
    List<TemplateEntity> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * fork 新版本后同步当前版本号。
     */
    @Update("""
            update templates
            set current_version_no = #{versionNo}
            where id = #{templateId}
            """)
    int updateCurrentVersionNo(@Param("templateId") Long templateId, @Param("versionNo") Integer versionNo);
}
