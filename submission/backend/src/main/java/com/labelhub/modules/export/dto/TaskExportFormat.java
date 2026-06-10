package com.labelhub.modules.export.dto;

import com.labelhub.common.exception.BusinessException;

import java.util.Locale;

public enum TaskExportFormat {
    JSON("json", "application/json"),
    JSONL("jsonl", "application/x-ndjson"),
    CSV("csv", "text/csv"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final String extension;
    private final String contentType;

    TaskExportFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    public static TaskExportFormat parseOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return JSONL;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("EXCEL".equals(normalized)) {
            return XLSX;
        }
        try {
            return TaskExportFormat.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400102, "Unsupported export format");
        }
    }
}
