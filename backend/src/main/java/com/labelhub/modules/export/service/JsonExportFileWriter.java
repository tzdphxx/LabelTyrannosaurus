package com.labelhub.modules.export.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.export.domain.ExportFormat;
import com.labelhub.modules.export.dto.ExportFieldMapping;
import com.labelhub.modules.submission.dto.ExportableSubmissionSnapshot;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * JSON 导出写入器。
 */
@Component
public class JsonExportFileWriter implements ExportFileWriter {

    private final ObjectMapper objectMapper;

    public JsonExportFileWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ExportFormat format() {
        return ExportFormat.JSON;
    }

    @Override
    public String contentType() {
        return "application/json";
    }

    @Override
    public ExportFileWriteSession open(OutputStream outputStream, List<ExportFieldMapping> fieldMappings) throws IOException {
        JsonGenerator generator = objectMapper.getFactory().createGenerator(outputStream);
        List<ExportFieldMapping> normalized = ExportWriterSupport.normalizedMappings(fieldMappings);
        generator.writeStartArray();
        return new ExportFileWriteSession() {
            @Override
            public void writeRow(ExportableSubmissionSnapshot snapshot) throws IOException {
                generator.writeTree(ExportWriterSupport.toRowNode(objectMapper, snapshot, normalized));
            }

            @Override
            public void close() throws IOException {
                generator.writeEndArray();
                generator.flush();
                generator.close();
            }
        };
    }
}
