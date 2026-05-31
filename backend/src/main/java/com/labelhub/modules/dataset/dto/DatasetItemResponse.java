package com.labelhub.modules.dataset.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

/**
 * 题目列表响应项。
 */
public record DatasetItemResponse(Long itemId,
                                  Long taskId,
                                  String externalId,
                                  String datasetType,
                                  JsonNode itemJson,
                                  JsonNode metadataJson,
                                  Integer assignedCount,
                                  Integer submittedCount,
                                  Integer approvedCount,
                                  LocalDateTime createdAt,
                                  LocalDateTime updatedAt) {
}
