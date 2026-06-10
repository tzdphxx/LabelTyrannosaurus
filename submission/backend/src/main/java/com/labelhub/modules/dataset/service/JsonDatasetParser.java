package com.labelhub.modules.dataset.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.dataset.domain.DatasetFileFormat;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON 数组数据集解析器。
 *
 * <p>源文件必须是对象数组；数组元素逐条转换为统一导入行，元素级异常会进入错误报告。</p>
 */
@Component
public class JsonDatasetParser extends AbstractDatasetParser {

    public JsonDatasetParser(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public DatasetFileFormat format() {
        return DatasetFileFormat.JSON;
    }

    @Override
    public DatasetParseResult parse(InputStream inputStream) {
        List<DatasetImportRow> rows = new ArrayList<>();
        List<DatasetImportError> errors = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(inputStream);
            if (!root.isArray()) {
                throw new IllegalArgumentException("JSON 数据集必须是数组");
            }
            int rowNo = 0;
            for (JsonNode rawRow : root) {
                rowNo++;
                try {
                    rows.add(toRow(rowNo, rawRow));
                } catch (Exception ex) {
                    errors.add(invalidRow(rowNo, ex.getMessage(), rawRow));
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("读取 JSON 数据集失败", ex);
        }
        return new DatasetParseResult(rows, errors);
    }
}
