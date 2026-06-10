package com.labelhub.modules.assignment.service;

public interface AssignmentMarketStatsService {

    Integer countClaimedByLabeler(Long taskId, Long labelerId);
}
