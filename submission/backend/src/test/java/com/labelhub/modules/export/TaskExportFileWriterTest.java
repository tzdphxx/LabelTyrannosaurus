package com.labelhub.modules.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.export.dto.TaskExportFormat;
import com.labelhub.modules.export.dto.TaskExportRow;
import com.labelhub.modules.export.service.TaskExportFileWriter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TaskExportFileWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TaskExportFileWriter writer = new TaskExportFileWriter(objectMapper);

    @Test
    void jsonAndJsonlIncludeRawItemAndAnswerJson() throws Exception {
        var json = writer.write(TaskExportFormat.JSON, List.of(row()));
        var jsonl = writer.write(TaskExportFormat.JSONL, List.of(row()));

        assertThat(json.contentType()).isEqualTo("application/json");
        assertThat(new String(json.bytes(), StandardCharsets.UTF_8))
                .contains("\"itemSnapshot\":{\"text\":\"raw\"}")
                .contains("\"answerJson\":{\"label\":\"ok\"}");
        assertThat(jsonl.contentType()).isEqualTo("application/x-ndjson");
        assertThat(new String(jsonl.bytes(), StandardCharsets.UTF_8).split("\\R")).hasSize(1);
        assertThat(new String(jsonl.bytes(), StandardCharsets.UTF_8))
                .contains("\"itemSnapshot\":{\"text\":\"raw\"}")
                .contains("\"answerJson\":{\"label\":\"ok\"}");
    }

    @Test
    void csvEscapesComplexJsonFields() throws Exception {
        var file = writer.write(TaskExportFormat.CSV, List.of(row()));

        String content = new String(file.bytes(), StandardCharsets.UTF_8);
        assertThat(file.contentType()).isEqualTo("text/csv");
        assertThat(content).startsWith("taskId,submissionId,datasetItemId,labelerId,versionNo,submittedAt,itemSnapshot,answerJson,aiReviewSnapshot,reviewComment");
        assertThat(content).contains("\"{\"\"text\"\":\"\"raw\"\"}\"");
        assertThat(content).contains("\"{\"\"label\"\":\"\"ok\"\"}\"");
    }

    @Test
    void xlsxCanBeReadBackWithPoi() throws Exception {
        var file = writer.write(TaskExportFormat.XLSX, List.of(row()));

        assertThat(file.contentType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(file.bytes()))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("taskId");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("1");
            assertThat(sheet.getRow(1).getCell(6).getStringCellValue()).contains("\"text\":\"raw\"");
            assertThat(sheet.getRow(1).getCell(7).getStringCellValue()).contains("\"label\":\"ok\"");
        }
    }

    private TaskExportRow row() {
        return new TaskExportRow(
                1L,
                200L,
                20L,
                10L,
                3,
                LocalDateTime.parse("2026-06-01T10:00:00"),
                objectMapper.valueToTree(Map.of("text", "raw")),
                objectMapper.valueToTree(Map.of("label", "ok")),
                objectMapper.valueToTree(Map.of("decision", "PASS")),
                "review, \"ok\""
        );
    }
}
