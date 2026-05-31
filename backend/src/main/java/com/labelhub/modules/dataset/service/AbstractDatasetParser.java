package com.labelhub.modules.dataset.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.labelhub.modules.dataset.domain.DatasetType;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据集解析器公共基类。
 *
 * <p>不同文件格式解析后都要归一化为 {@link DatasetImportRow}：业务唯一键
 * {@code externalId} 单独提取，展示/标注 payload 写入 {@code itemJson}，
 * 附加信息写入 {@code metadataJson}。</p>
 */
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

    /**
     * 将单行原始 JSON 转换为统一导入行。
     *
     * <p>仅 {@code externalId} 是导入协议字段，不进入题目内容；{@code metadata}
     * 作为元数据单独存储，其余字段完整保留给后续标注工作台展示。</p>
     */
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
