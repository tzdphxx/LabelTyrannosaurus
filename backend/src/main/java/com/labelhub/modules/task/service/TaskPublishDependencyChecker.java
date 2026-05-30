package com.labelhub.modules.task.service;

/**
 * Boundary owned by BE-B integrations. BE-A calls this interface before publishing a task, but the
 * default implementation must not pretend external dataset, template, or reward resources exist.
 */
public interface TaskPublishDependencyChecker {

    boolean datasetReady(Long taskId);

    boolean templateVersionExists(Long templateVersionId);

    boolean aiReviewConfigExists(Long taskId, Long aiReviewConfigId);

    boolean rewardRuleExists(Long taskId);
}
