package com.labelhub.modules.export.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.export.domain.ExportFormat;
import com.labelhub.modules.export.dto.ExportFieldMapping;
import com.labelhub.modules.submission.dto.ExportableSubmissionSnapshot;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JSONL 导出写入器。
 */
@Component
public class JsonlExportFileWriter implements ExportFileWriter {

    private final ObjectMapper objectMapper;

    public JsonlExportFileWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ExportFormat format() {
        return ExportFormat.JSONL;
    }

    @Override
    public String contentType() {
        return "application/x-ndjson";
    }

    @Override
    public ExportFileWriteSession open(OutputStream outputStream, List<ExportFieldMapping> fieldMappings) {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        List<ExportFieldMapping> normalized = ExportWriterSupport.normalizedMappings(fieldMappings);
        return new ExportFileWriteSession() {
            @Override
            public void writeRow(ExportableSubmissionSnapshot snapshot) throws IOException {
                writer.write(objectMapper.writeValueAsString(ExportWriterSupport.toRowNode(objectMapper, snapshot, normalized)));
                writer.newLine();
            }

            @Override
            public void close() throws IOException {
                writer.flush();
            }
        };
    }
}
