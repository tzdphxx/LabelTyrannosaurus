package com.labelhub.modules.dataset.domain;

import java.util.Locale;

public enum DatasetFileFormat {
    JSON,
    JSONL,
    EXCEL,
    CSV;

    public static DatasetFileFormat fromFilename(String filename) {
        String normalized = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".jsonl")) {
            return JSONL;
        }
        if (normalized.endsWith(".json")) {
            return JSON;
        }
        if (normalized.endsWith(".xlsx") || normalized.endsWith(".xls")) {
            return EXCEL;
        }
        if (normalized.endsWith(".csv")) {
            return CSV;
        }
        throw new IllegalArgumentException("Unsupported dataset file format");
    }
}
