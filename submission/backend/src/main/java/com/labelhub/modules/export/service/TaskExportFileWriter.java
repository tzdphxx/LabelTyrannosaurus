package com.labelhub.modules.export.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.export.dto.GeneratedTaskExportFile;
import com.labelhub.modules.export.dto.TaskExportFormat;
import com.labelhub.modules.export.dto.TaskExportRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class TaskExportFileWriter {

    private static final List<String> HEADERS = List.of(
            "taskId",
            "submissionId",
            "datasetItemId",
            "labelerId",
            "versionNo",
            "submittedAt",
            "itemSnapshot",
            "answerJson",
            "aiReviewSnapshot",
            "reviewComment"
    );

    private final ObjectMapper objectMapper;

    public TaskExportFileWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public GeneratedTaskExportFile write(TaskExportFormat format, List<TaskExportRow> rows) {
        TaskExportFormat resolvedFormat = format == null ? TaskExportFormat.JSONL : format;
        byte[] bytes = switch (resolvedFormat) {
            case JSON -> writeJson(rows);
            case JSONL -> writeJsonl(rows);
            case CSV -> writeCsv(rows);
            case XLSX -> writeXlsx(rows);
        };
        return new GeneratedTaskExportFile(resolvedFormat.contentType(), resolvedFormat.extension(), bytes);
    }

    private byte[] writeJson(List<TaskExportRow> rows) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             JsonGenerator generator = objectMapper.getFactory().createGenerator(outputStream)) {
            generator.writeStartArray();
            for (TaskExportRow row : rows) {
                generator.writeTree(toNode(row));
            }
            generator.writeEndArray();
            generator.flush();
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException(500001, "Failed to generate export JSON file");
        }
    }

    private byte[] writeJsonl(List<TaskExportRow> rows) {
        StringBuilder builder = new StringBuilder();
        try {
            for (TaskExportRow row : rows) {
                builder.append(objectMapper.writeValueAsString(toNode(row))).append('\n');
            }
            return builder.toString().getBytes(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BusinessException(500001, "Failed to generate export JSONL file");
        }
    }

    private byte[] writeCsv(List<TaskExportRow> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.join(",", HEADERS)).append('\n');
        for (TaskExportRow row : rows) {
            List<String> values = values(row).stream().map(this::escapeCsv).toList();
            builder.append(String.join(",", values)).append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] writeXlsx(List<TaskExportRow> rows) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("approved-submissions");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) {
                header.createCell(i).setCellValue(HEADERS.get(i));
            }
            for (int i = 0; i < rows.size(); i++) {
                Row xlsxRow = sheet.createRow(i + 1);
                List<String> values = values(rows.get(i));
                for (int j = 0; j < values.size(); j++) {
                    xlsxRow.createCell(j).setCellValue(values.get(j));
                }
            }
            workbook.write(outputStream);
            workbook.dispose();
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException(500001, "Failed to generate export XLSX file");
        }
    }

    private ObjectNode toNode(TaskExportRow row) {
        ObjectNode node = objectMapper.createObjectNode();
        putNumber(node, "taskId", row.taskId());
        putNumber(node, "submissionId", row.submissionId());
        putNumber(node, "datasetItemId", row.datasetItemId());
        putNumber(node, "labelerId", row.labelerId());
        if (row.versionNo() == null) {
            node.putNull("versionNo");
        } else {
            node.put("versionNo", row.versionNo());
        }
        if (row.submittedAt() == null) {
            node.putNull("submittedAt");
        } else {
            node.put("submittedAt", row.submittedAt().toString());
        }
        node.set("itemSnapshot", nullToNullNode(row.itemSnapshot()));
        node.set("answerJson", nullToNullNode(row.answerJson()));
        node.set("aiReviewSnapshot", nullToNullNode(row.aiReviewSnapshot()));
        if (row.reviewComment() == null) {
            node.putNull("reviewComment");
        } else {
            node.put("reviewComment", row.reviewComment());
        }
        return node;
    }

    private List<String> values(TaskExportRow row) {
        return List.of(
                stringify(row.taskId()),
                stringify(row.submissionId()),
                stringify(row.datasetItemId()),
                stringify(row.labelerId()),
                stringify(row.versionNo()),
                row.submittedAt() == null ? "" : row.submittedAt().toString(),
                stringify(row.itemSnapshot()),
                stringify(row.answerJson()),
                stringify(row.aiReviewSnapshot()),
                row.reviewComment() == null ? "" : row.reviewComment()
        );
    }

    private void putNumber(ObjectNode node, String field, Long value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }

    private JsonNode nullToNullNode(JsonNode value) {
        return value == null ? objectMapper.nullNode() : value;
    }

    private String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof JsonNode jsonNode) {
            if (jsonNode.isNull()) {
                return "";
            }
            if (jsonNode.isTextual()) {
                return jsonNode.asText();
            }
            try {
                return objectMapper.writeValueAsString(jsonNode);
            } catch (IOException ex) {
                throw new BusinessException(500001, "Failed to serialize export field");
            }
        }
        return String.valueOf(value);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String safeValue = neutralizeSpreadsheetFormula(value);
        boolean quote = safeValue.contains(",") || safeValue.contains("\"")
                || safeValue.contains("\n") || safeValue.contains("\r");
        String escaped = safeValue.replace("\"", "\"\"");
        return quote ? "\"" + escaped + "\"" : escaped;
    }

    private String neutralizeSpreadsheetFormula(String value) {
        if (value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            return "'" + value;
        }
        return value;
    }
}
