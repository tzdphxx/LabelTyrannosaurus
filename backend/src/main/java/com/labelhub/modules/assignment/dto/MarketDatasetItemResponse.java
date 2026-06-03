package com.labelhub.modules.assignment.dto;

public record MarketDatasetItemResponse(Long datasetItemId,
                                        String externalId,
                                        String itemJson,
                                        String metadataJson) {
}
