package com.labelhub.modules.dataset.service;

import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import org.springframework.stereotype.Service;

@Service
public class DefaultDatasetMarketStatsService implements DatasetMarketStatsService {

    private final DatasetItemMapper datasetItemMapper;

    public DefaultDatasetMarketStatsService(DatasetItemMapper datasetItemMapper) {
        this.datasetItemMapper = datasetItemMapper;
    }

    @Override
    public Integer countAvailableItems(Long taskId, Long labelerId, Integer overlapCount) {
        return datasetItemMapper.countAvailableForLabeler(taskId, labelerId, overlapCount);
    }
}
