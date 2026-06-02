package com.labelhub.modules.reward.service;

import com.labelhub.modules.assignment.dto.RewardSummaryResponse;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class DefaultRewardSummaryService implements RewardSummaryService {

    private final TaskMapper taskMapper;

    public DefaultRewardSummaryService(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Override
    public RewardSummaryResponse findRewardSummary(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return null;
        }
        if (task.getRewardPerApproval() == null && task.getPenaltyPerRejection() == null) {
            return null;
        }
        return new RewardSummaryResponse(
                task.getRewardPerApproval() != null ? task.getRewardPerApproval() : BigDecimal.ZERO,
                task.getPenaltyPerRejection() != null ? task.getPenaltyPerRejection() : BigDecimal.ZERO,
                task.getBonusThreshold(),
                task.getBonusPoints()
        );
    }
}
