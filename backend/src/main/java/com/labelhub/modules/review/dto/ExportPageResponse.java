package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "导出分页响应")
public record ExportPageResponse(
        @Schema(description = "黄金标准条目列表") List<ExportGoldenItem> items,
        @Schema(description = "下一页游标") Long nextCursor,
        @Schema(description = "是否有更多") boolean hasMore
) {
}
