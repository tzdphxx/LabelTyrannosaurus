package com.labelhub.modules.ai.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.ai.dto.LlmTriggerRunPageResponse;
import com.labelhub.modules.ai.dto.LlmTriggerRunQuery;
import com.labelhub.modules.ai.dto.LlmTriggerRunResponse;
import com.labelhub.modules.ai.mapper.LlmTriggerRunMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LlmTriggerRunQueryService {

    private static final int TASK_NOT_FOUND = 404001;
    private static final int FORBIDDEN = 403001;

    private final LlmTriggerRunMapper llmTriggerRunMapper;
    private final TaskMapper taskMapper;
    private final LlmTriggerService llmTriggerService;

    public LlmTriggerRunQueryService(LlmTriggerRunMapper llmTriggerRunMapper,
                                     TaskMapper taskMapper,
                                     LlmTriggerService llmTriggerService) {
        this.llmTriggerRunMapper = llmTriggerRunMapper;
        this.taskMapper = taskMapper;
        this.llmTriggerService = llmTriggerService;
    }

    public LlmTriggerRunPageResponse listByTask(CurrentUser currentUser,
                                                LlmTriggerRunQuery query) {
        Task task = taskMapper.selectById(query.taskId());
        if (task == null) {
            throw new BusinessException(TASK_NOT_FOUND, "任务不存在");
        }
        requireAccess(currentUser, task);

        long total = llmTriggerRunMapper.countByTaskId(query);
        List<LlmTriggerRunResponse> items = llmTriggerRunMapper
                .selectPageByTaskId(query)
                .stream()
                .map(llmTriggerService::toRunResponse)
                .toList();

        return new LlmTriggerRunPageResponse(
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
        throw new BusinessException(FORBIDDEN, "无权查看 LLM 调用日志");
    }
}