package com.labelhub.modules.export.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.export.domain.ExportFormat;
import com.labelhub.modules.export.dto.ExportFieldMapping;
import com.labelhub.modules.submission.dto.ExportableSubmissionSnapshot;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Excel 导出写入器。
 */
@Component
public class ExcelExportFileWriter implements ExportFileWriter {

    private static final int MAX_ROWS_PER_SHEET = 1_000_000;

    private final ObjectMapper objectMapper;

    public ExcelExportFileWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ExportFormat format() {
        return ExportFormat.EXCEL;
    }

    @Override
    public String contentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public ExportFileWriteSession open(OutputStream outputStream, List<ExportFieldMapping> fieldMappings) {
        SXSSFWorkbook workbook = new SXSSFWorkbook();
        List<ExportFieldMapping> normalized = ExportWriterSupport.normalizedMappings(fieldMappings);
        Sheet[] sheetHolder = new Sheet[]{workbook.createSheet("export-1")};
        int[] rowIndex = new int[]{0};
        writeHeader(sheetHolder[0], normalized);
        rowIndex[0] = 1;
        return new ExportFileWriteSession() {
            @Override
            public void writeRow(ExportableSubmissionSnapshot snapshot) throws IOException {
                if (rowIndex[0] >= MAX_ROWS_PER_SHEET) {
                    sheetHolder[0] = workbook.createSheet("export-" + (workbook.getNumberOfSheets() + 1));
                    writeHeader(sheetHolder[0], normalized);
                    rowIndex[0] = 1;
                }
                Row row = sheetHolder[0].createRow(rowIndex[0]++);
                List<String> values = ExportWriterSupport.csvValues(objectMapper, snapshot, normalized);
                for (int i = 0; i < values.size(); i++) {
                    row.createCell(i).setCellValue(values.get(i));
                }
            }

            @Override
            public void close() throws IOException {
                try {
                    workbook.write(outputStream);
                    outputStream.flush();
                } finally {
                    workbook.close();
                    workbook.dispose();
                }
            }
        };
    }

    private void writeHeader(Sheet sheet, List<ExportFieldMapping> fieldMappings) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < fieldMappings.size(); i++) {
            header.createCell(i).setCellValue(fieldMappings.get(i).targetName());
        }
    }
}
