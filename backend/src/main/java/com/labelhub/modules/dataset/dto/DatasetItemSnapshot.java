package com.labelhub.modules.dataset.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 给 BE-A 领取、提交和渲染链路使用的题目快照。
 *
 * <p>快照只表达数据集资产的稳定内容，不携带 assignment 或 submission 状态。</p>
 */
public record DatasetItemSnapshot(Long itemId,
                                  Long taskId,
                                  String externalId,
                                  String datasetType,
                                  JsonNode itemJson,
                                  JsonNode metadataJson) {
}
