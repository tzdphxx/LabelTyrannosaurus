package com.labelhub.modules.dataset.domain;

/**
 * 数据集导入任务状态。
 */
public enum DatasetImportStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    PARTIAL_SUCCESS
}
