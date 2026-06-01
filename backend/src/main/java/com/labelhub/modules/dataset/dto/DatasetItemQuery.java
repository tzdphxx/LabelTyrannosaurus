package com.labelhub.modules.dataset.dto;

/**
 * 题目列表查询参数。
 *
 * @param page 页码，从 1 开始
 * @param pageSize 每页条数
 * @param externalId 可选 externalId 查询关键字
 */
public record DatasetItemQuery(Integer page,
                               Integer pageSize,
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
