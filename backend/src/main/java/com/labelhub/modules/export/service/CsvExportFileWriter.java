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
 * CSV 导出写入器。
 */
@Component
public class CsvExportFileWriter implements ExportFileWriter {

    private final ObjectMapper objectMapper;

    public CsvExportFileWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ExportFormat format() {
        return ExportFormat.CSV;
    }

    @Override
    public String contentType() {
        return "text/csv";
    }

    @Override
    public ExportFileWriteSession open(OutputStream outputStream, List<ExportFieldMapping> fieldMappings) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        List<ExportFieldMapping> normalized = ExportWriterSupport.normalizedMappings(fieldMappings);
        writer.write(String.join(",", normalized.stream().map(ExportFieldMapping::targetName).toList()));
        writer.newLine();
        return new ExportFileWriteSession() {
            @Override
            public void writeRow(ExportableSubmissionSnapshot snapshot) throws IOException {
                List<String> values = ExportWriterSupport.csvValues(objectMapper, snapshot, normalized);
                writer.write(String.join(",", values.stream().map(CsvExportFileWriter.this::escape).toList()));
                writer.newLine();
            }

            @Override
            public void close() throws IOException {
                writer.flush();
            }
        };
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needQuote ? "\"" + escaped + "\"" : escaped;
    }
}
