package com.labelhub.modules.reward.repository;

/**
 * 奖励结算读取的提交快照。该对象来自 BE-A 的 submissions 表，BE-B 只能只读依赖。
 */
public record SubmissionSnapshot(Long submissionId,
                                 Long assignmentId,
                                 Long taskId,
                                 Long datasetItemId,
                                 Long labelerId) {
}
