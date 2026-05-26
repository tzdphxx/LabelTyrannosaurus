package com.labelhub.infrastructure.storage;

public interface ObjectStorageService {

    String upload(String bucket, String objectKey, String contentType, byte[] content);
}
