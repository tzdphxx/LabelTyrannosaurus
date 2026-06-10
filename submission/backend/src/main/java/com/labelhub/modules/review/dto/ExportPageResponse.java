package com.labelhub.modules.review.dto;

import java.util.List;

public record ExportPageResponse(
        List<ExportGoldenItem> items,
        Long nextCursor,
        boolean hasMore
) {
}
