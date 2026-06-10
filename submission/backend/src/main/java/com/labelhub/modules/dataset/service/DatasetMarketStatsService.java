package com.labelhub.modules.dataset.service;

public interface DatasetMarketStatsService {

    Integer countAvailableItems(Long taskId, Long labelerId, Integer overlapCount);
}
