package com.labelhub.infrastructure.storage;

import java.io.InputStream;
import java.net.URL;
import java.time.Instant;

public interface ObjectStorageService {

    /**
     * 上传对象内容到对象存储。
     *
     * @param bucket 存储桶名称
     * @param objectKey 对象 key，由业务层统一生成
     * @param contentType 文件 MIME 类型
     * @param content 文件输入流，调用方负责传入可读流
     * @param contentLength 文件字节长度
     */
    void upload(String bucket, String objectKey, String contentType, InputStream content, long contentLength);

    /**
     * 打开对象读取流，供后端异步任务读取已上传的源文件。
     *
     * <p>返回流由调用方负责关闭，避免 COS HTTP 连接泄漏。</p>
     */
    InputStream openReadStream(String bucket, String objectKey);

    /**
     * 生成短期下载签名 URL。
     *
     * @param originalFilename 用于 Content-Disposition 的下载文件名
     */
    URL generatePresignedDownloadUrl(String bucket, String objectKey, String originalFilename, Instant expiresAt);
}
