package com.labelhub.modules.template.domain;

/**
 * 模板版本状态。
 *
 * <p>当前数据库用 {@code published_snapshot} 布尔值存储发布快照语义，本枚举只作为业务层展示和判断使用。</p>
 */
public enum TemplateVersionState {
    DRAFT,
    PUBLISHED_SNAPSHOT
}
