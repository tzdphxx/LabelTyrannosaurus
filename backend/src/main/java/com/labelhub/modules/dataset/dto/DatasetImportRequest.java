package com.labelhub.modules.dataset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 创建数据集导入任务请求。
 *
 * @param fileId 已上传到对象存储的文件 id
 */
@Schema(description = "创建数据集导入任务请求")
public record DatasetImportRequest(
        @NotNull @Schema(description = "已上传到对象存储的文件ID") Long fileId) {
}
