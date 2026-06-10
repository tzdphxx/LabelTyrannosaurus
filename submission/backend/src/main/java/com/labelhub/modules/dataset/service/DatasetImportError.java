package com.labelhub.modules.dataset.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 单行导入失败信息。
 *
 * <p>该结构会序列化进 JSONL 错误报告，便于前端展示和用户修正源文件。</p>
 */
public record DatasetImportError(int rowNo,
                                 String externalId,
                                 String errorCode,
                                 String errorMessage,
                                 JsonNode rawRow) {
}
