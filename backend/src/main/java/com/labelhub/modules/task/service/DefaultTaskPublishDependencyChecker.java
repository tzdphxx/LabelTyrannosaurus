package com.labelhub.modules.task.service;

import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import org.springframework.stereotype.Component;

@Component
public class DefaultTaskPublishDependencyChecker implements TaskPublishDependencyChecker {

    private final AiReviewConfigMapper aiReviewConfigMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final TemplateVersionMapper templateVersionMapper;

    public DefaultTaskPublishDependencyChecker(AiReviewConfigMapper aiReviewConfigMapper,
                                               DatasetItemMapper datasetItemMapper,
                                               TemplateVersionMapper templateVersionMapper) {
        this.aiReviewConfigMapper = aiReviewConfigMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.templateVersionMapper = templateVersionMapper;
    }

    @Override
    public boolean datasetReady(Long taskId) {
        return datasetItemMapper.countByTaskId(taskId) > 0;
    }

    @Override
    public boolean templateVersionExists(Long templateVersionId) {
        return templateVersionMapper.selectById(templateVersionId) != null;
    }

    @Override
    public boolean aiReviewConfigExists(Long taskId, Long aiReviewConfigId) {
        if (taskId == null || aiReviewConfigId == null) {
            return false;
        }
        AiReviewConfig config = aiReviewConfigMapper.selectById(aiReviewConfigId);
        return config != null && taskId.equals(config.getTaskId());
    }

}
