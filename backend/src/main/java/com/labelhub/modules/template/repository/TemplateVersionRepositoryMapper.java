package com.labelhub.modules.template.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.template.domain.TemplateVersionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 模板版本表 Mapper。
 */
@Mapper
public interface TemplateVersionRepositoryMapper extends BaseMapper<TemplateVersionEntity> {

    /**
     * 查询模板的指定版本。
     */
    @Select("""
            select * from template_versions
            where template_id = #{templateId}
              and version_no = #{versionNo}
            limit 1
            """)
    TemplateVersionEntity selectByTemplateIdAndVersionNo(@Param("templateId") Long templateId,
                                                         @Param("versionNo") Integer versionNo);
}
