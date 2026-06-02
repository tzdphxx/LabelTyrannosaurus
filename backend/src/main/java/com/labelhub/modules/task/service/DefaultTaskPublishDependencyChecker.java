package com.labelhub.modules.task.service;

import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.reward.mapper.RewardRuleMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.template.domain.TemplateVersion;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import org.springframework.stereotype.Component;

@Component
public class DefaultTaskPublishDependencyChecker implements TaskPublishDependencyChecker {

    private final AiReviewConfigMapper aiReviewConfigMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final TaskMapper taskMapper;
    private final TemplateVersionMapper templateVersionMapper;
    private final RewardRuleMapper rewardRuleMapper;

    public DefaultTaskPublishDependencyChecker(TaskMapper taskMapper,
                                               AiReviewConfigMapper aiReviewConfigMapper,
                                               DatasetItemMapper datasetItemMapper,
                                               TemplateVersionMapper templateVersionMapper,
                                               RewardRuleMapper rewardRuleMapper) {
        this.taskMapper = taskMapper;
        this.aiReviewConfigMapper = aiReviewConfigMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.templateVersionMapper = templateVersionMapper;
        this.rewardRuleMapper = rewardRuleMapper;
    }

    @Override
    public boolean datasetReady(Long taskId) {
        return datasetItemMapper.countByTaskId(taskId) > 0;
    }

    @Override
    public boolean templateVersionOwnedBy(Long ownerId, Long templateVersionId) {
        if (ownerId == null || templateVersionId == null) {
            return false;
        }
        TemplateVersion version = templateVersionMapper.selectById(templateVersionId);
        return version != null && ownerId.equals(version.getOwnerId());
    }

    @Override
    public boolean templateVersionUsableByTask(Long taskId, Long templateVersionId) {
        if (taskId == null || templateVersionId == null) {
            return false;
        }
        Task task = taskMapper.selectById(taskId);
        return task != null && templateVersionOwnedBy(task.getOwnerId(), templateVersionId);
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
        return rewardRuleMapper.countByTaskId(taskId) > 0;
    }
}
