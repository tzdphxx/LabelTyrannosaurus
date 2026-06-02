package com.labelhub.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "分页响应包装")
public record PageResponse<T>(
        @Schema(description = "当前页数据列表")
        List<T> items,
        @Schema(description = "当前页码，从 1 开始", example = "1")
        int page,
        @Schema(description = "每页条数", example = "20")
        int pageSize,
        @Schema(description = "总记录数", example = "100")
        long total
) {
}
