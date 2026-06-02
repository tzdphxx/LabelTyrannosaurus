package com.labelhub.modules.ai.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.dto.AiReviewResultPageResponse;
import com.labelhub.modules.ai.dto.AiReviewResultQuery;
import com.labelhub.modules.ai.dto.AiReviewResultResponse;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiReviewLogQueryService {

    private static final int TASK_NOT_FOUND = 404001;
    private static final int FORBIDDEN = 403001;

    private final AiReviewResultMapper aiReviewResultMapper;
    private final TaskMapper taskMapper;
    private final AiAutoReviewService aiAutoReviewService;

    public AiReviewLogQueryService(AiReviewResultMapper aiReviewResultMapper,
                                   TaskMapper taskMapper,
                                   AiAutoReviewService aiAutoReviewService) {
        this.aiReviewResultMapper = aiReviewResultMapper;
        this.taskMapper = taskMapper;
        this.aiAutoReviewService = aiAutoReviewService;
    }

    public AiReviewResultPageResponse listByTask(CurrentUser currentUser,
                                                 AiReviewResultQuery query) {
        Task task = taskMapper.selectById(query.taskId());
        if (task == null) {
            throw new BusinessException(TASK_NOT_FOUND, "Task not found");
        }
        requireAccess(currentUser, task);

        long total = aiReviewResultMapper.countByTaskId(query);
        List<AiReviewResultResponse> items = aiReviewResultMapper
                .selectPageByTaskId(query)
                .stream()
                .map(aiAutoReviewService::toResponse)
                .toList();

        return new AiReviewResultPageResponse(
                items, query.normalizedPage(), query.normalizedPageSize(), total);
    }

    private void requireAccess(CurrentUser currentUser, Task task) {
        if (currentUser.roles().contains(RoleCode.ADMIN)) {
            return;
        }
        if (currentUser.roles().contains(RoleCode.OWNER)
                && currentUser.userId().equals(task.getOwnerId())) {
            return;
        }
        if (currentUser.roles().contains(RoleCode.REVIEWER)) {
            return;
        }
        throw new BusinessException(FORBIDDEN, "No permission to view AI review logs");
    }
}
