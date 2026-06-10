package com.labelhub.modules.dataset.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 创建数据集导入任务请求。
 *
 * @param fileId 已上传到对象存储的文件 id
 */
public record DatasetImportRequest(@NotNull Long fileId) {
}
