package com.labelhub.modules.dataset.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "题目摘要")
public record ItemSummaryResponse(
        @Schema(description = "题目 ID", example = "100")
        Long itemId,
        @Schema(description = "题目业务编号", example = "q1")
        String externalId,
        @Schema(description = "题目内容 JSON")
        String itemJson,
        @Schema(description = "题目元数据 JSON")
        String metadataJson
) {}
