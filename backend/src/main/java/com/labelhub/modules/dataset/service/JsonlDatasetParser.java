package com.labelhub.modules.dataset.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.dataset.domain.DatasetFileFormat;
import com.labelhub.modules.dataset.domain.DatasetType;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * JSONL 数据集解析器。
 *
 * <p>每一行独立解析，单行 JSON 非法或缺少 {@code externalId} 时只记录该行错误，
 * 不影响其他行继续导入。</p>
 */
@Component
public class JsonlDatasetParser extends AbstractDatasetParser {

    public JsonlDatasetParser(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public DatasetFileFormat format() {
        return DatasetFileFormat.JSONL;
    }

    @Override
    public DatasetParseResult parse(InputStream inputStream, DatasetType datasetType) {
        List<DatasetImportRow> rows = new ArrayList<>();
        List<DatasetImportError> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int rowNo = 0;
            while ((line = reader.readLine()) != null) {
                rowNo++;
                if (line.isBlank()) {
                    continue;
                }
                JsonNode rawRow = null;
                try {
                    rawRow = objectMapper.readTree(line);
                    rows.add(toRow(rowNo, rawRow, datasetType));
                } catch (Exception ex) {
                    errors.add(invalidRow(rowNo, ex.getMessage(), rawRow));
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Read JSONL dataset failed", ex);
        }
        return new DatasetParseResult(rows, errors);
    }
}
