package com.labelhub.modules.export.domain;

/**
 * 导出文件格式枚举。枚举值必须和 `export_jobs.export_format` 约束保持一致。
 */
public enum ExportFormat {
    JSON("json", "application/json"),
    JSONL("jsonl", "application/x-ndjson"),
    CSV("csv", "text/csv"),
    EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final String extension;
    private final String contentType;

    ExportFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }
}
