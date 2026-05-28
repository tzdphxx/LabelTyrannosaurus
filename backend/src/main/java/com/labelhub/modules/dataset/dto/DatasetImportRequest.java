package com.labelhub.modules.dataset.dto;

import com.labelhub.modules.dataset.domain.DatasetType;
import jakarta.validation.constraints.NotNull;

/**
 * 创建数据集导入任务请求。
 *
 * @param fileId 已上传到对象存储的文件 id
 * @param datasetType 数据集业务类型
 */
public record DatasetImportRequest(@NotNull Long fileId, @NotNull DatasetType datasetType) {
}
