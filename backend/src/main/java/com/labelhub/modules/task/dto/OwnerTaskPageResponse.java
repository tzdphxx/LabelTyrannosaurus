package com.labelhub.modules.task.dto;

import com.labelhub.modules.task.domain.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;

public record OwnerTaskPageResponse(List<OwnerTaskSummaryResponse> items,
                                    int page,
                                    int pageSize,
                                    long total) {
}
