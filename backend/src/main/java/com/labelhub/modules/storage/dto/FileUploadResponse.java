package com.labelhub.modules.storage.dto;

public record FileUploadResponse(Long fileId,
                                 String originalFilename,
                                 String contentType,
                                 Long fileSize,
                                 String objectKey,
                                 String downloadUrl) {
}
