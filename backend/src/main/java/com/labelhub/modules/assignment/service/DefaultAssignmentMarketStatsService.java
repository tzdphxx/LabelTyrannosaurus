package com.labelhub.modules.assignment.service;

import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import org.springframework.stereotype.Service;

@Service
public class DefaultAssignmentMarketStatsService implements AssignmentMarketStatsService {

    private final AssignmentMapper assignmentMapper;

    public DefaultAssignmentMarketStatsService(AssignmentMapper assignmentMapper) {
        this.assignmentMapper = assignmentMapper;
    }

    @Override
    public Integer countClaimedByLabeler(Long taskId, Long labelerId) {
        return assignmentMapper.countByTaskAndLabeler(taskId, labelerId);
    }
}
