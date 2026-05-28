package com.labelhub.modules.task.service;

import org.springframework.stereotype.Component;

@Component
public class DefaultTaskPublishDependencyChecker implements TaskPublishDependencyChecker {

    @Override
    public boolean datasetReady(Long taskId) {
        return false;
    }

    @Override
    public boolean templateVersionExists(Long templateVersionId) {
        return false;
    }

    @Override
    public boolean rewardRuleExists(Long taskId) {
        return false;
    }
}
