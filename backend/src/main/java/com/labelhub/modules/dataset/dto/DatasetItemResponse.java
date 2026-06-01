package com.labelhub.modules.dataset.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

/**
 * 题目列表响应项。
 */
public record DatasetItemResponse(Long itemId,
                                  Long taskId,
                                  String externalId,
                                  JsonNode itemJson,
                                  JsonNode metadataJson,
                                  Integer assignedCount,
                                  Integer submittedCount,
                                  Integer approvedCount,
                                  DatasetItemStatus itemStatus,
                                  Long labelerId,
                                  LocalDateTime createdAt,
                                  LocalDateTime updatedAt) {
}
