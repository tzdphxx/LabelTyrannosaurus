package com.labelhub.modules.template.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.template.domain.TemplateVersionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    @Select("""
            select * from template_versions
            where template_id = #{templateId}
            order by version_no desc, id desc
            """)
    List<TemplateVersionEntity> selectByTemplateIdOrderByVersionNoDesc(@Param("templateId") Long templateId);

    /**
     * 任务发布后标记该版本已被发布任务引用，后续只能 fork 新版本。
     */
    @Update("""
            update template_versions
            set published_snapshot = true
            where id = #{versionId}
            """)
    int markPublishedSnapshot(@Param("versionId") Long versionId);
}
