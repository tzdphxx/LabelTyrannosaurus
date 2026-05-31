package com.labelhub.modules.dataset.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.labelhub.modules.dataset.domain.DatasetFileFormat;
import com.labelhub.modules.dataset.domain.DatasetType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 数据集解析器。
 *
 * <p>第一行作为字段名表头；普通列进入 {@code itemJson}，
 * 以 {@code metadata.} 开头的列进入 {@code metadataJson}。</p>
 */
@Component
public class ExcelDatasetParser extends AbstractDatasetParser {

    public ExcelDatasetParser(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public DatasetFileFormat format() {
        return DatasetFileFormat.EXCEL;
    }

    @Override
    public DatasetParseResult parse(InputStream inputStream, DatasetType datasetType) {
        List<DatasetImportRow> rows = new ArrayList<>();
        List<DatasetImportError> errors = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (var workbook = WorkbookFactory.create(inputStream)) {
            var sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) {
                throw new IllegalArgumentException("Excel dataset header is required");
            }
            List<String> headers = new ArrayList<>();
            for (Cell cell : header) {
                headers.add(formatter.formatCellValue(cell).trim());
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row excelRow = sheet.getRow(i);
                if (excelRow == null) {
                    continue;
                }
                ObjectNode rawRow = objectMapper.createObjectNode();
                ObjectNode metadata = objectMapper.createObjectNode();
                for (int c = 0; c < headers.size(); c++) {
                    String headerName = headers.get(c);
                    if (headerName.isBlank()) {
                        continue;
                    }
                    String value = formatter.formatCellValue(excelRow.getCell(c));
                    if (headerName.startsWith("metadata.")) {
                        metadata.put(headerName.substring("metadata.".length()), value);
                    } else {
                        rawRow.put(headerName, value);
                    }
                }
                if (!metadata.isEmpty()) {
                    rawRow.set("metadata", metadata);
                }
                try {
                    rows.add(toRow(i + 1, rawRow, datasetType));
                } catch (Exception ex) {
                    errors.add(invalidRow(i + 1, ex.getMessage(), rawRow));
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Read Excel dataset failed", ex);
        }
        return new DatasetParseResult(rows, errors);
    }
}
