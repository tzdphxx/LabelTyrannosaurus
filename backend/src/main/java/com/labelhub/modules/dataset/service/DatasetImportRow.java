package com.labelhub.modules.dataset.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 解析后的标准导入行。
 *
 * @param rowNo 源文件中的行号或数组序号，用于错误报告定位
 * @param externalId 同一任务内的业务唯一键
 * @param itemJson 写入 dataset_items.item_json 的题目内容
 * @param metadataJson 写入 dataset_items.metadata_json 的附加信息
 * @param rawRow 原始行快照，用于错误报告回显
 */
public record DatasetImportRow(int rowNo,
                               String externalId,
                               JsonNode itemJson,
                               JsonNode metadataJson,
                               JsonNode rawRow) {
}
