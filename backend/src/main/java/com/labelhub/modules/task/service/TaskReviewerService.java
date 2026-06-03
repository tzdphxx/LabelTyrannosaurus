package com.labelhub.modules.task.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.auth.repository.UserRoleMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskReviewer;
import com.labelhub.modules.task.dto.TaskReviewerResponse;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.task.mapper.TaskReviewerMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskReviewerService {

    private static final int TASK_NOT_FOUND = 404001;
    private static final int INVALID_REVIEWER = 400103;

    private final TaskMapper taskMapper;
    private final TaskReviewerMapper taskReviewerMapper;
    private final UserRoleMapper userRoleMapper;

    public TaskReviewerService(TaskMapper taskMapper,
                               TaskReviewerMapper taskReviewerMapper,
                               UserRoleMapper userRoleMapper) {
        this.taskMapper = taskMapper;
        this.taskReviewerMapper = taskReviewerMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Transactional
    public void assignReviewers(Long ownerId, Long taskId,
                                List<Long> reviewerIds) {
        loadOwnedTask(ownerId, taskId);
        for (Long reviewerId : reviewerIds) {
            validateReviewer(reviewerId);
        }
        taskReviewerMapper.delete(
                new QueryWrapper<TaskReviewer>().eq("task_id", taskId));
        for (Long reviewerId : reviewerIds) {
            TaskReviewer tr = new TaskReviewer();
            tr.setTaskId(taskId);
            tr.setReviewerId(reviewerId);
            tr.setAssignedBy(ownerId);
            tr.setCreatedAt(LocalDateTime.now());
            taskReviewerMapper.insert(tr);
        }
    }

    public List<TaskReviewerResponse> getReviewers(Long ownerId, Long taskId) {
        loadOwnedTask(ownerId, taskId);
        return taskReviewerMapper.selectReviewerDetails(taskId);
    }

    private void validateReviewer(Long reviewerId) {
        java.util.Set<com.labelhub.common.security.RoleCode> roles =
                userRoleMapper.selectRoleCodesByUserId(reviewerId);
        if (roles == null || !roles.contains(com.labelhub.common.security.RoleCode.REVIEWER)) {
            throw new BusinessException(INVALID_REVIEWER,
                    "User " + reviewerId + " is not a reviewer");
        }
    }

    private Task loadOwnedTask(Long ownerId, Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || (!CurrentUserContext.isAdmin() && !ownerId.equals(task.getOwnerId()))) {
            throw new BusinessException(TASK_NOT_FOUND, "任务不存在");
        }
        return task;
    }
}
