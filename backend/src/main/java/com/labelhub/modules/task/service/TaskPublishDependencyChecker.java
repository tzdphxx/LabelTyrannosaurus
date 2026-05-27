package com.labelhub.modules.task.service;

public interface TaskPublishDependencyChecker {

    boolean datasetReady(Long taskId);

    boolean templateVersionExists(Long templateVersionId);

    boolean rewardRuleExists(Long taskId);
}
