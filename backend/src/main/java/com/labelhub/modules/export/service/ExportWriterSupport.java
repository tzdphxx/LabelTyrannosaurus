package com.labelhub.modules.export.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.export.dto.ExportFieldMapping;
import com.labelhub.modules.submission.dto.ExportableSubmissionSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * 导出写入公共工具。
 */
final class ExportWriterSupport {

    private static final List<ExportFieldMapping> DEFAULT_MAPPINGS = List.of(
            new ExportFieldMapping("$.submissionId", "submissionId", null, true),
            new ExportFieldMapping("$.datasetItemId", "datasetItemId", null, true),
            new ExportFieldMapping("$.itemSnapshot", "itemSnapshot", null, true),
            new ExportFieldMapping("$.answerJson", "answerJson", null, true),
            new ExportFieldMapping("$.aiReviewSnapshot", "aiReviewSnapshot", null, true),
            new ExportFieldMapping("$.auditRefs", "auditRefs", null, true),
            new ExportFieldMapping("$.reviewComment", "reviewComment", null, true),
            new ExportFieldMapping("$.labelerInfo", "labelerInfo", null, true)
    );

    private ExportWriterSupport() {
    }

    static List<ExportFieldMapping> normalizedMappings(List<ExportFieldMapping> fieldMappings) {
        if (fieldMappings == null || fieldMappings.isEmpty()) {
            return DEFAULT_MAPPINGS;
        }
        return fieldMappings.stream()
                .filter(mapping -> mapping.include() == null || mapping.include())
                .toList();
    }

    static ObjectNode toRowNode(ObjectMapper objectMapper,
                                ExportableSubmissionSnapshot snapshot,
                                List<ExportFieldMapping> fieldMappings) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("submissionId", snapshot.submissionId());
        root.put("datasetItemId", snapshot.datasetItemId());
        root.set("itemSnapshot", snapshot.itemSnapshot() == null ? objectMapper.nullNode() : snapshot.itemSnapshot());
        root.set("answerJson", snapshot.answerJson() == null ? objectMapper.nullNode() : snapshot.answerJson());
        root.set("aiReviewSnapshot", snapshot.aiReviewSnapshot() == null ? objectMapper.nullNode() : snapshot.aiReviewSnapshot());
        root.set("auditRefs", snapshot.auditRefs() == null ? objectMapper.nullNode() : objectMapper.valueToTree(snapshot.auditRefs()));
        root.putNull("reviewComment");
        if (snapshot.reviewComment() != null) {
            root.put("reviewComment", snapshot.reviewComment());
        }
        root.set("labelerInfo", snapshot.labelerInfo() == null ? objectMapper.nullNode() : objectMapper.valueToTree(snapshot.labelerInfo()));
        if (snapshot.submittedAt() != null) {
            root.put("submittedAt", snapshot.submittedAt().toString());
        } else {
            root.putNull("submittedAt");
        }
        ObjectNode row = objectMapper.createObjectNode();
        for (ExportFieldMapping mapping : fieldMappings) {
            row.set(mapping.targetName(), extractNode(root, mapping.sourceJsonPath(), objectMapper));
        }
        return row;
    }

    static List<String> csvHeaders(List<ExportFieldMapping> fieldMappings) {
        return normalizedMappings(fieldMappings).stream().map(ExportFieldMapping::targetName).toList();
    }

    static List<String> csvValues(ObjectMapper objectMapper,
                                  ExportableSubmissionSnapshot snapshot,
                                  List<ExportFieldMapping> fieldMappings) {
        ObjectNode row = toRowNode(objectMapper, snapshot, fieldMappings);
        List<String> values = new ArrayList<>();
        row.fieldNames().forEachRemaining(name -> values.add(stringify(row.get(name), objectMapper)));
        return values;
    }

    static String stringify(JsonNode node, ObjectMapper objectMapper) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            throw new BusinessException(500001, "Export row serialization failed");
        }
    }

    private static JsonNode extractNode(JsonNode root, String sourceJsonPath, ObjectMapper objectMapper) {
        if (sourceJsonPath == null || sourceJsonPath.isBlank() || !sourceJsonPath.startsWith("$")) {
            throw new BusinessException(400102, "Invalid export source path");
        }
        if ("$".equals(sourceJsonPath)) {
            return root;
        }
        String path = sourceJsonPath.startsWith("$.") ? sourceJsonPath.substring(2) : sourceJsonPath.substring(1);
        JsonNode current = root;
        for (String segment : path.split("\\.")) {
            if (segment.isBlank()) {
                throw new BusinessException(400102, "Invalid export source path");
            }
            if (current == null || current.isNull()) {
                return objectMapper.nullNode();
            }
            current = current.get(segment);
        }
        return current == null ? objectMapper.nullNode() : current;
    }
}
