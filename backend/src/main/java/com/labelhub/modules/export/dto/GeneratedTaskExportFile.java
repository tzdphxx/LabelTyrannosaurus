package com.labelhub.modules.export.dto;

public record GeneratedTaskExportFile(String contentType,
                                      String extension,
                                      byte[] bytes) {

    public long size() {
        return bytes == null ? 0L : bytes.length;
    }
}
