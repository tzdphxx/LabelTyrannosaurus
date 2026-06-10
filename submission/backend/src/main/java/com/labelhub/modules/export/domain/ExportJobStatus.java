package com.labelhub.modules.export.domain;

/**
 * 导出任务状态。必须和数据库 `export_jobs.status` 约束一致。
 */
public enum ExportJobStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED
}
