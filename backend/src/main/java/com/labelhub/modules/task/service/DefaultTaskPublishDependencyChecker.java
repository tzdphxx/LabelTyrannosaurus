package com.labelhub.modules.task.service;

import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import org.springframework.stereotype.Component;

@Component
public class DefaultTaskPublishDependencyChecker implements TaskPublishDependencyChecker {

    private final AiReviewConfigMapper aiReviewConfigMapper;

    public DefaultTaskPublishDependencyChecker(AiReviewConfigMapper aiReviewConfigMapper) {
        this.aiReviewConfigMapper = aiReviewConfigMapper;
    }

    @Override
    public boolean datasetReady(Long taskId) {
        return false;
    }

    @Override
    public boolean templateVersionExists(Long templateVersionId) {
        return false;
    }

    @Override
    public boolean aiReviewConfigExists(Long taskId, Long aiReviewConfigId) {
        if (taskId == null || aiReviewConfigId == null) {
            return false;
        }
        AiReviewConfig config = aiReviewConfigMapper.selectById(aiReviewConfigId);
        return config != null && taskId.equals(config.getTaskId());
    }

    @Override
    public boolean rewardRuleExists(Long taskId) {
        return false;
    }
}
