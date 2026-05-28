package com.labelhub.modules.dataset.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.labelhub.modules.dataset.domain.DatasetType;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractDatasetParser implements DatasetParser {

    protected final ObjectMapper objectMapper;

    protected AbstractDatasetParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected List<DatasetImportError> errors() {
        return new ArrayList<>();
    }

    protected DatasetImportError invalidRow(int rowNo, String message, JsonNode rawRow) {
        String externalId = rawRow != null && rawRow.hasNonNull("externalId") ? rawRow.get("externalId").asText() : null;
        return new DatasetImportError(rowNo, externalId, "INVALID_ROW", message, rawRow);
    }

    protected DatasetImportRow toRow(int rowNo, JsonNode rawRow, DatasetType datasetType) {
        if (!rawRow.isObject()) {
            throw new IllegalArgumentException("Dataset row must be a JSON object");
        }
        JsonNode externalIdNode = rawRow.get("externalId");
        String externalId = externalIdNode == null || externalIdNode.asText().isBlank()
                ? ""
                : externalIdNode.asText().trim();
        if (externalId.isBlank()) {
            throw new IllegalArgumentException("externalId is required");
        }
        ObjectNode itemJson = rawRow.deepCopy();
        JsonNode metadataJson = itemJson.remove("metadata");
        itemJson.remove("externalId");
        if (metadataJson == null || metadataJson.isNull()) {
            metadataJson = objectMapper.createObjectNode();
        }
        itemJson.put("datasetType", datasetType.name());
        return new DatasetImportRow(rowNo, externalId, itemJson, metadataJson, rawRow);
    }
}
