package com.labelhub.modules.dataset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.dataset.domain.DatasetType;
import com.labelhub.modules.dataset.service.ExcelDatasetParser;
import com.labelhub.modules.dataset.service.JsonDatasetParser;
import com.labelhub.modules.dataset.service.JsonlDatasetParser;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void jsonlParserKeepsValidRowsAndRecordsBadLines() {
        JsonlDatasetParser parser = new JsonlDatasetParser(objectMapper);
        String content = """
                {"externalId":"q1","question":"one","metadata":{"source":"seed"}}
                not-json
                {"externalId":"","question":"missing"}
                {"externalId":"q2","prompt":"two"}
                """;

        var result = parser.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), DatasetType.QA_QUALITY);

        assertThat(result.rows()).hasSize(2);
        assertThat(result.rows().get(0).rowNo()).isEqualTo(1);
        assertThat(result.rows().get(0).externalId()).isEqualTo("q1");
        assertThat(result.rows().get(0).itemJson().get("question").asText()).isEqualTo("one");
        assertThat(result.rows().get(0).metadataJson().get("source").asText()).isEqualTo("seed");
        assertThat(result.rows().get(1).externalId()).isEqualTo("q2");
        assertThat(result.errors()).extracting("rowNo").containsExactly(2, 3);
    }

    @Test
    void jsonParserReadsArrayOfObjects() {
        JsonDatasetParser parser = new JsonDatasetParser(objectMapper);
        String content = """
                [
                  {"externalId":"p1","left":"A","right":"B"},
                  {"externalId":"p2","left":"C","right":"D"}
                ]
                """;

        var result = parser.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), DatasetType.PREFERENCE_COMPARE);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).hasSize(2);
        assertThat(result.rows()).extracting("externalId").containsExactly("p1", "p2");
        assertThat(result.rows().get(0).itemJson().get("left").asText()).isEqualTo("A");
    }

    @Test
    void excelParserUsesFirstRowAsHeader() throws Exception {
        ExcelDatasetParser parser = new ExcelDatasetParser(objectMapper);
        XSSFWorkbook workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("dataset");
        var header = sheet.createRow(0);
        header.createCell(0).setCellValue("externalId");
        header.createCell(1).setCellValue("question");
        header.createCell(2).setCellValue("metadata.source");
        var row = sheet.createRow(1);
        row.createCell(0).setCellValue("q1");
        row.createCell(1).setCellValue("How good is this answer?");
        row.createCell(2).setCellValue("manual");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        var result = parser.parse(new ByteArrayInputStream(out.toByteArray()), DatasetType.QA_QUALITY);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0).externalId()).isEqualTo("q1");
        assertThat(result.rows().get(0).itemJson().get("question").asText()).isEqualTo("How good is this answer?");
        assertThat(result.rows().get(0).metadataJson().get("source").asText()).isEqualTo("manual");
    }
}
