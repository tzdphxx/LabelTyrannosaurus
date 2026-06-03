package com.labelhub.modules.template.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.template.domain.TemplateVersion;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import org.springframework.stereotype.Service;

@Service
public class DefaultTemplateSchemaService implements TemplateSchemaService {

    private static final int TEMPLATE_VERSION_NOT_FOUND = 404201;

    private final TemplateVersionMapper templateVersionMapper;

    public DefaultTemplateSchemaService(TemplateVersionMapper templateVersionMapper) {
        this.templateVersionMapper = templateVersionMapper;
    }

    @Override
    public TemplateSchemaSnapshot getTemplateSchema(Long templateVersionId) {
        TemplateVersion templateVersion = templateVersionMapper.selectById(templateVersionId);
        if (templateVersion == null) {
            throw new BusinessException(TEMPLATE_VERSION_NOT_FOUND, "模板版本不存在");
        }
        return new TemplateSchemaSnapshot(templateVersion.getId(), templateVersion.getSchemaJson());
    }
}
