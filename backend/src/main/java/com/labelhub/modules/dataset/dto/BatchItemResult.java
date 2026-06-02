package com.labelhub.modules.dataset.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 题目批量操作的逐条结果。
 *
 * <p>批量编辑采用部分成功策略，前端可以根据该结果展示每一行的成功状态和失败原因。</p>
 */
@Schema(description = "题目批量操作的逐条结果")
public record BatchItemResult(
        @Schema(description = "题目ID") Long itemId,
        @Schema(description = "外部ID") String externalId,
        @Schema(description = "是否成功") boolean success,
        @Schema(description = "错误码") Integer errorCode,
        @Schema(description = "错误信息") String errorMessage) {

    public static BatchItemResult success(Long itemId, String externalId) {
        return new BatchItemResult(itemId, externalId, true, null, null);
    }

    public static BatchItemResult failure(Long itemId, String externalId, int errorCode, String errorMessage) {
        return new BatchItemResult(itemId, externalId, false, errorCode, errorMessage);
    }
}
