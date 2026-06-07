package com.labelhub.modules.dataset.dto;

/**
 * 题目批量操作的逐条结果。
 *
 * <p>批量编辑采用部分成功策略，前端可以根据该结果展示每一行的成功状态和失败原因。</p>
 */
public record BatchItemResult(Long itemId,
                              String externalId,
                              boolean success,
                              Integer errorCode,
                              String errorMessage) {

    public static BatchItemResult success(Long itemId, String externalId) {
        return new BatchItemResult(itemId, externalId, true, null, null);
    }

    public static BatchItemResult failure(Long itemId, String externalId, int errorCode, String errorMessage) {
        return new BatchItemResult(itemId, externalId, false, errorCode, errorMessage);
    }
}
