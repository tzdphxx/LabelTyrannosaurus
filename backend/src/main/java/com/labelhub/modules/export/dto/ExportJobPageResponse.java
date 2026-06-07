package com.labelhub.modules.export.dto;

import java.util.List;

/**
 * 导出任务分页响应。
 */
public record ExportJobPageResponse(List<ExportJobResponse> items,
                                    int page,
                                    int pageSize,
                                    long total) {
}
