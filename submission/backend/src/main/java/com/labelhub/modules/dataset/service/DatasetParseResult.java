package com.labelhub.modules.dataset.service;

import java.util.List;

/**
 * 数据集解析结果。
 *
 * <p>解析阶段只负责格式和必填字段校验，跨行唯一性等业务规则由导入服务处理。</p>
 */
public record DatasetParseResult(List<DatasetImportRow> rows, List<DatasetImportError> errors) {
}
