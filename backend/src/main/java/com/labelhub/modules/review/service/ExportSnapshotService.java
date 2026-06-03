package com.labelhub.modules.review.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.review.dto.ExportGoldenItem;
import com.labelhub.modules.review.dto.ExportPageRequest;
import com.labelhub.modules.review.dto.ExportPageResponse;
import com.labelhub.modules.review.mapper.ExportSubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExportSnapshotService {

    private static final int TASK_NOT_FOUND = 404801;
    private static final int TASK_NOT_OWNED = 403801;

    private final ExportSubmissionMapper exportSubmissionMapper;
    private final TaskMapper taskMapper;

    public ExportSnapshotService(ExportSubmissionMapper exportSubmissionMapper,
                                  TaskMapper taskMapper) {
        this.exportSubmissionMapper = exportSubmissionMapper;
        this.taskMapper = taskMapper;
    }

    public ExportPageResponse queryExportableGoldenSubmissions(Long ownerId,
                                                               ExportPageRequest request) {
        Task task = taskMapper.selectById(request.taskId());
        if (task == null) {
            throw new BusinessException(TASK_NOT_FOUND, "任务不存在");
        }
        if (!task.getOwnerId().equals(ownerId)) {
            throw new BusinessException(TASK_NOT_OWNED, "当前账号不是任务负责人");
        }

        int fetchLimit = request.limit() + 1;
        List<ExportGoldenItem> items = exportSubmissionMapper.selectGoldenPage(
                request.taskId(), request.lastId(), fetchLimit);

        boolean hasMore = items.size() > request.limit();
        if (hasMore) {
            items = items.subList(0, request.limit());
        }

        Long nextCursor = items.isEmpty() ? null : items.get(items.size() - 1).submissionId();

        return new ExportPageResponse(items, nextCursor, hasMore);
    }
}
