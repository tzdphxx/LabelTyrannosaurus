package com.labelhub.modules.dataset.dto;

import com.labelhub.modules.dataset.domain.DatasetType;

/**
 * 题目列表查询参数。
 *
 * @param page 页码，从 1 开始
 * @param pageSize 每页条数
 * @param datasetType 可选数据集类型过滤
 * @param externalId 可选 externalId 查询关键字
 */
public record DatasetItemQuery(Integer page,
                               Integer pageSize,
                               DatasetType datasetType,
                               String externalId) {

    public int normalizedPage() {
        return page == null || page < 1 ? 1 : page;
    }

    public int normalizedPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }

    public int offset() {
        return (normalizedPage() - 1) * normalizedPageSize();
    }
}
