package com.labelhub.modules.dataset.domain;

/**
 * 数据集业务类型。
 *
 * <p>与任务模板和前端工作台的数据展示形态对应，新增类型需要同步数据库约束和接口契约。</p>
 */
public enum DatasetType {
    QA_QUALITY,
    PREFERENCE_COMPARE
}
