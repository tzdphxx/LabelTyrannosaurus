package com.labelhub.modules.dataset.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 题目列表查询参数。
 *
 * @param page 页码，从 1 开始
 * @param pageSize 每页条数
 * @param externalId 可选 externalId 查询关键字
 */
@Schema(description = "题目列表查询参数")
public record DatasetItemQuery(
        @Schema(description = "页码，从1开始") Integer page,
        @Schema(description = "每页条数") Integer pageSize,
        @Schema(description = "外部ID查询关键字") String externalId) {

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
