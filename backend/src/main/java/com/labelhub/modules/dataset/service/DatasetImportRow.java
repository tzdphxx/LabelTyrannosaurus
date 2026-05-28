package com.labelhub.modules.dataset.service;

import com.fasterxml.jackson.databind.JsonNode;

public record DatasetImportRow(int rowNo,
                               String externalId,
                               JsonNode itemJson,
                               JsonNode metadataJson,
                               JsonNode rawRow) {
}
