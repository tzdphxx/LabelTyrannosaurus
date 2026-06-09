package com.labelhub.modules.export.dto;

public record DirectTaskExportResponse(Long fileId,
                                       String filename,
                                       String contentType,
                                       Long fileSize,
                                       String checksum,
                                       String downloadUrl,
                                       Integer exportedCount) {
}
