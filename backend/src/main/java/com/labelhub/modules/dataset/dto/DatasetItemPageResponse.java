package com.labelhub.modules.dataset.dto;

import java.util.List;

/**
 * 题目分页响应。
 */
public record DatasetItemPageResponse(List<DatasetItemResponse> items,
                                      int page,
                                      int pageSize,
                                      long total) {
}
