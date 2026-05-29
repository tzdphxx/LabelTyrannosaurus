package com.labelhub.modules.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.export.domain.ExportFormat;
import com.labelhub.modules.export.dto.ExportFieldMapping;
import com.labelhub.modules.export.service.CsvExportFileWriter;
import com.labelhub.modules.export.service.JsonlExportFileWriter;
import com.labelhub.modules.submission.dto.ExportableSubmissionSnapshot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExportFileWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void jsonlWriterUsesFieldMapping() throws Exception {
        JsonlExportFileWriter writer = new JsonlExportFileWriter(objectMapper);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (var session = writer.open(output, List.of(
                new ExportFieldMapping("$.submissionId", "submission_id", null, true),
                new ExportFieldMapping("$.answerJson.answer", "answer", null, true)
        ))) {
            session.writeRow(snapshot());
        }

        String content = output.toString(StandardCharsets.UTF_8);
        assertThat(writer.format()).isEqualTo(ExportFormat.JSONL);
        assertThat(content).contains("\"submission_id\":200");
        assertThat(content).contains("\"answer\":\"A, \\\"quoted\\\"\"");
        assertThat(content).doesNotContain("datasetItemId");
    }

    @Test
    void csvWriterEscapesMappedValues() throws Exception {
        CsvExportFileWriter writer = new CsvExportFileWriter(objectMapper);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (var session = writer.open(output, List.of(
                new ExportFieldMapping("$.submissionId", "submission_id", null, true),
                new ExportFieldMapping("$.answerJson.answer", "answer", null, true)
        ))) {
            session.writeRow(snapshot());
        }

        String content = output.toString(StandardCharsets.UTF_8);
        assertThat(writer.format()).isEqualTo(ExportFormat.CSV);
        assertThat(content).startsWith("submission_id,answer");
        assertThat(content).contains("200,\"A, \"\"quoted\"\"\"");
    }

    private ExportableSubmissionSnapshot snapshot() {
        return new ExportableSubmissionSnapshot(
                200L,
                11L,
                objectMapper.valueToTree(Map.of("question", "What is A?")),
                objectMapper.valueToTree(Map.of("answer", "A, \"quoted\"")),
                objectMapper.valueToTree(Map.of("decision", "PASS")),
                List.of(),
                null,
                null,
                LocalDateTime.parse("2026-05-01T10:00:00")
        );
    }
}
